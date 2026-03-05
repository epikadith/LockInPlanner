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
        val allowedRootKeys = setOf("tasks", "checklists", "shorts", "books")
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

        // Validate Shorts
        if (root.has("shorts")) {
             val shortsArray = root.get("shorts")
             if (!shortsArray.isJsonArray) throw Exception("'shorts' must be an array.")
             shortsArray.asJsonArray.forEach { element ->
                 if (!element.isJsonObject) throw Exception("Short item must be an object.")
                 validateShortSchema(element.asJsonObject)
             }
        }

        // Validate Books
        if (root.has("books")) {
             val booksArray = root.get("books")
             if (!booksArray.isJsonArray) throw Exception("'books' must be an array.")
             booksArray.asJsonArray.forEach { element ->
                 if (!element.isJsonObject) throw Exception("Book wrapper must be an object.")
                 validateBookSchema(element.asJsonObject)
             }
        }

        // If validation passes, deserialize
        return gson.fromJson(jsonElement, ExportData::class.java)
    }

    private fun validateTaskSchema(json: JsonObject) {
         val allowed = setOf("id", "name", "description", "color", "repeatability", 
                             "customRepeatDays", "isFloating", "startTime", "endTime", "reminders", "isThemeColor", "tag")
         val required = setOf("name", "startTime", "endTime") // Minimal required
         
         val keys = json.keySet()
         val unknown = keys.filter { !allowed.contains(it) }
         if (unknown.isNotEmpty()) throw Exception("Unknown fields in Task: $unknown")
         
         val missing = required.filter { !keys.contains(it) }
         if (missing.isNotEmpty()) throw Exception("Missing required fields in Task: $missing")
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

    private fun validateShortSchema(json: JsonObject) {
         val allowed = setOf("id", "title", "content", "colorArgb", "createdAt")
         val required = setOf("title")
         val keys = json.keySet()
         
         val unknown = keys.filter { !allowed.contains(it) }
         if (unknown.isNotEmpty()) throw Exception("Unknown fields in Short: $unknown")
         
         val missing = required.filter { !keys.contains(it) }
         if (missing.isNotEmpty()) throw Exception("Missing required fields in Short: $missing")
    }

    private fun validateBookSchema(json: JsonObject) {
         val allowedWrapper = setOf("book", "chapters")
         val keys = json.keySet()
         val unknown = keys.filter { !allowedWrapper.contains(it) }
         if (unknown.isNotEmpty()) throw Exception("Unknown fields in Book Wrapper: $unknown")
         
         if (!json.has("book")) throw Exception("Book wrapper missing 'book' object.")
         validateBookEntitySchema(json.get("book").asJsonObject)
         
         if (json.has("chapters")) {
             val chapters = json.get("chapters")
             if (!chapters.isJsonArray) throw Exception("'chapters' must be an array.")
             chapters.asJsonArray.forEach { 
                 if (!it.isJsonObject) throw Exception("Chapter wrapper must be an object.")
                 validateChapterSchema(it.asJsonObject) 
             }
         }
    }
    
    private fun validateBookEntitySchema(json: JsonObject) {
        val allowed = setOf("id", "title", "createdAt", "colorArgb")
        val required = setOf("title")
        val keys = json.keySet()
        
        val unknown = keys.filter { !allowed.contains(it) }
        if (unknown.isNotEmpty()) throw Exception("Unknown fields in Book Entity: $unknown")
        
        val missing = required.filter { !keys.contains(it) }
        if (missing.isNotEmpty()) throw Exception("Missing required fields in Book Entity: $missing")
    }

    private fun validateChapterSchema(json: JsonObject) {
        val allowedWrapper = setOf("chapter", "pages")
        val keys = json.keySet()
        val unknown = keys.filter { !allowedWrapper.contains(it) }
        if (unknown.isNotEmpty()) throw Exception("Unknown fields in Chapter Wrapper: $unknown")
        
        if (!json.has("chapter")) throw Exception("Chapter wrapper missing 'chapter' object.")
        validateChapterEntitySchema(json.get("chapter").asJsonObject)
        
        if (json.has("pages")) {
            val pages = json.get("pages")
            if (!pages.isJsonArray) throw Exception("'pages' must be an array.")
            pages.asJsonArray.forEach { 
                if (!it.isJsonObject) throw Exception("Page Entity must be an object.")
                validatePageEntitySchema(it.asJsonObject) 
            }
        }
    }

    private fun validateChapterEntitySchema(json: JsonObject) {
        val allowed = setOf("id", "bookId", "title", "createdAt", "orderIndex")
        val required = setOf("title")
        val keys = json.keySet()
        
        val unknown = keys.filter { !allowed.contains(it) }
        if (unknown.isNotEmpty()) throw Exception("Unknown fields in Chapter Entity: $unknown")
        
        val missing = required.filter { !keys.contains(it) }
        if (missing.isNotEmpty()) throw Exception("Missing required fields in Chapter Entity: $missing")
    }

    private fun validatePageEntitySchema(json: JsonObject) {
        val allowed = setOf("id", "chapterId", "title", "content", "createdAt", "orderIndex")
        val required = setOf("title", "content")
        val keys = json.keySet()
        
        val unknown = keys.filter { !allowed.contains(it) }
        if (unknown.isNotEmpty()) throw Exception("Unknown fields in Page Entity: $unknown")
        
        val missing = required.filter { !keys.contains(it) }
        if (missing.isNotEmpty()) throw Exception("Missing required fields in Page Entity: $missing")
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
                    var isThemeColor = false
                    if (tokens.size >= 11) {
                         isThemeColor = tokens[10].toBoolean()
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
                            reminders = reminders,
                            isThemeColor = isThemeColor
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
