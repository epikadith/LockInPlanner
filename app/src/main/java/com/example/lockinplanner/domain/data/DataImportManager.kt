package com.example.lockinplanner.domain.data

import android.net.Uri
import com.example.lockinplanner.data.local.entity.ChecklistEntity
import com.example.lockinplanner.data.local.entity.ObjectiveEntity
import com.example.lockinplanner.data.local.entity.TaskEntity
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.JsonObject
import com.google.gson.JsonArray
import java.io.InputStream
import java.io.InputStreamReader
import java.util.UUID

class DataImportManager {

    private val gson = Gson()

    sealed class ImportResult {
        data class Success(val data: ExportData) : ImportResult()
        data class Error(val message: String) : ImportResult()
    }

    fun importData(inputStream: InputStream, format: ExportFormat): ImportResult {
        return try {
            val data = when (format) {
                ExportFormat.JSON -> parseJson(inputStream)
                ExportFormat.CSV -> parseDelimiterSeparated(inputStream, ",")
                ExportFormat.TSV -> parseDelimiterSeparated(inputStream, "\t")
                else -> return ImportResult.Error("Unsupported format for import: $format")
            }
            
            // Validation
            if (data.tasks.any { it.name.isBlank() }) {
                return ImportResult.Error("Import failed: One or more tasks have empty names.")
            }
            if (data.tasks.any { it.startTime == it.endTime }) {
                return ImportResult.Error("Import failed: One or more tasks have zero duration (Start Time = End Time).")
            }
            if (data.checklists.any { it.checklist.name.isBlank() }) {
                return ImportResult.Error("Import failed: One or more checklists have empty names.")
            }
            // Additional check: Ensure IDs are present if required? 
            // Entities usually have default IDs if not provided, but Export should provide them.
            // If ID is missing in JSON, it might default to empty string if not nullable?
            // UUID.randomUUID().toString() is default in Entity constructor.
            // So if JSON misses 'id', it gets a random one. This is actually fine/safe.
            
            ImportResult.Success(data)
        } catch (e: Exception) {
            ImportResult.Error("Import failed: ${e.message}")
        }
    }

    private fun parseJson(inputStream: InputStream): ExportData {
        val reader = InputStreamReader(inputStream)
        val jsonElement = JsonParser.parseReader(reader)
        
        if (!jsonElement.isJsonObject) {
             throw Exception("Root must be a JSON object.")
        }
        val root = jsonElement.asJsonObject
        
        // Allowed root keys
        val allowedRootKeys = setOf("tasks", "checklists")
        val unknownRoot = root.keySet().filter { !allowedRootKeys.contains(it) }
        if (unknownRoot.isNotEmpty()) {
             throw Exception("Unknown fields in root: $unknownRoot")
        }

        // Validate Tasks
        if (root.has("tasks")) {
            val tasksArray = root.get("tasks")
            if (!tasksArray.isJsonArray) throw Exception("'tasks' must be an array.")
            tasksArray.asJsonArray.forEach { element ->
                if (!element.isJsonObject) throw Exception("Task must be an object.")
                validateTaskSchema(element.asJsonObject)
            }
        }

        // Validate Checklists
        if (root.has("checklists")) {
             val checklistsArray = root.get("checklists")
             if (!checklistsArray.isJsonArray) throw Exception("'checklists' must be an array.")
             checklistsArray.asJsonArray.forEach { element ->
                 if (!element.isJsonObject) throw Exception("Checklist item must be an object.")
                 validateChecklistSchema(element.asJsonObject)
             }
        }

        // If validation passes, deserialize
        return gson.fromJson(jsonElement, ExportData::class.java)
    }

    private fun validateTaskSchema(json: JsonObject) {
         val allowed = setOf("id", "name", "description", "color", "repeatability", 
                             "customRepeatDays", "isFloating", "startTime", "endTime", "reminders")
         val required = setOf("name", "startTime", "endTime") // Minimal required
         
         val keys = json.keySet()
         val unknown = keys.filter { !allowed.contains(it) }
         if (unknown.isNotEmpty()) throw Exception("Unknown fields in Task: $unknown")
         
         val missing = required.filter { !keys.contains(it) }
         if (missing.isNotEmpty()) throw Exception("Missing required fields in Task: $missing")
         
         // Logic check (already have duplicate check later, but good to catch here)
         if (json.get("startTime").asLong == json.get("endTime").asLong) {
              throw Exception("Task '${json.get("name").asString}' has zero duration.")
         }
    }

    private fun validateChecklistSchema(json: JsonObject) {
         val allowedWrapper = setOf("checklist", "items")
         val keys = json.keySet()
         val unknown = keys.filter { !allowedWrapper.contains(it) }
         if (unknown.isNotEmpty()) throw Exception("Unknown fields in Checklist Wrapper: $unknown")
         
         if (!json.has("checklist")) throw Exception("Checklist wrapper missing 'checklist' object.")
         validateChecklistEntitySchema(json.get("checklist").asJsonObject)
         
         if (json.has("items")) {
             val items = json.get("items")
             if (!items.isJsonArray) throw Exception("'items' must be an array.")
             items.asJsonArray.forEach { 
                 if (!it.isJsonObject) throw Exception("Objective must be an object.")
                 validateObjectiveSchema(it.asJsonObject) 
             }
         }
    }

    private fun validateChecklistEntitySchema(json: JsonObject) {
        val allowed = setOf("id", "name", "isCompleted", "createdAt")
        val required = setOf("name")
        val keys = json.keySet()
        
        val unknown = keys.filter { !allowed.contains(it) }
        if (unknown.isNotEmpty()) throw Exception("Unknown fields in Checklist Entity: $unknown")
        
        val missing = required.filter { !keys.contains(it) }
        if (missing.isNotEmpty()) throw Exception("Missing required fields in Checklist Entity: $missing")
    }

    private fun validateObjectiveSchema(json: JsonObject) {
        val allowed = setOf("id", "checklistId", "text", "isCompleted", "order")
        val required = setOf("text", "isCompleted") // Strict: 'isCrazy' will fail here due to 'unknown' AND 'missing isCompleted'
        val keys = json.keySet()
        
        val unknown = keys.filter { !allowed.contains(it) }
        if (unknown.isNotEmpty()) throw Exception("Unknown fields in Objective: $unknown")
        
        val missing = required.filter { !keys.contains(it) }
        if (missing.isNotEmpty()) throw Exception("Missing required fields in Objective: $missing")
    }

    private fun parseDelimiterSeparated(inputStream: InputStream, delimiter: String): ExportData {
        val reader = InputStreamReader(inputStream)
        val lines = reader.readLines()
        if (lines.isEmpty()) return ExportData()

        val header = lines.first()
        val tasks = mutableListOf<TaskEntity>()
        val checklistsMap = mutableMapOf<String, ChecklistEntity>()
        val itemsMap = mutableMapOf<String, MutableList<ObjectiveEntity>>()

        // Heuristic to detect content type based on header
        if (header.startsWith("ID${delimiter}Name")) {
            // Task CSV
            for (i in 1 until lines.size) {
                val line = lines[i]
                if (line.isBlank()) continue
                // Note: Basic split prevents handling commas inside quotes correctly if simplistic.
                // Robust parsing requires regex or character loop.
                // But DataExportManager uses quotes. So we MUST handle quotes.
                val tokens = parseLine(line, delimiter[0])
                if (tokens.size >= 7) {
                    // Start with defaults
                    var color: Long = 0xFFFFFFFF
                    var customRepeat: Int? = null
                    var reminders = emptyList<Int>()

                    // Attempt to parse extended fields if available
                    if (tokens.size >= 8) color = tokens[7].toLongOrNull() ?: 0xFFFFFFFF
                    if (tokens.size >= 9) customRepeat = tokens[8].toIntOrNull()
                    if (tokens.size >= 10) {
                        reminders = tokens[9].split(";").mapNotNull { it.toIntOrNull() }
                    }

                    tasks.add(
                        TaskEntity(
                            id = 0, // Auto-generate new ID
                            name = tokens[1],
                            description = tokens[2].takeIf { it.isNotBlank() },
                            startTime = tokens[3].toLongOrNull() ?: 0L,
                            endTime = tokens[4].toLongOrNull() ?: 0L,
                            repeatability = tokens[5],
                            isFloating = tokens[6].toBoolean(),
                            color = color,
                            customRepeatDays = customRepeat,
                            reminders = reminders
                        )
                    )
                }
            }
        } else if (header.startsWith("ChecklistID${delimiter}ChecklistName")) {
            // Checklist CSV
            for (i in 1 until lines.size) {
                 val line = lines[i]
                 if (line.isBlank()) continue
                 val tokens = parseLine(line, delimiter[0])
                 if (tokens.size >= 6) {
                     val cId = tokens[0] 
                     // We need to map old IDs to new IDs to maintain relationships?
                     // Or just generate new UUIDs?
                     // If rows repeat the same ChecklistID, they belong to same list.
                     if (!checklistsMap.containsKey(cId)) {
                         checklistsMap[cId] = ChecklistEntity(
                             id = UUID.randomUUID().toString(), // New ID
                             name = tokens[1],
                             isCompleted = tokens[2].toBoolean()
                         )
                         itemsMap[cId] = mutableListOf()
                     }
                     
                     // If item exists
                     if (tokens[3].isNotBlank()) {
                         itemsMap[cId]?.add(
                             ObjectiveEntity(
                                 id = UUID.randomUUID().toString(),
                                 checklistId = checklistsMap[cId]!!.id, // Use the NEW ID
                                 text = tokens[4],
                                 isCompleted = tokens[5].toBoolean()
                             )
                         )
                     }
                 }
            }
        }
        
        val checklists = checklistsMap.map { (oldId, entity) ->
            ChecklistWithItems(entity, itemsMap[oldId] ?: emptyList())
        }

        return ExportData(tasks, checklists)
    }

    private fun parseLine(line: String, separator: Char): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        for (c in line) {
            if (c == '\"') {
                inQuotes = !inQuotes
            } else if (c == separator && !inQuotes) {
                tokens.add(sb.toString())
                sb.clear()
            } else {
                sb.append(c)
            }
        }
        tokens.add(sb.toString())
        return tokens
    }
}
