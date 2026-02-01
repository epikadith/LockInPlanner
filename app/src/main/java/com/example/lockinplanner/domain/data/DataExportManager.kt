package com.example.lockinplanner.domain.data

import com.example.lockinplanner.data.local.entity.ChecklistEntity
import com.example.lockinplanner.data.local.entity.ObjectiveEntity
import com.example.lockinplanner.data.local.entity.TaskEntity
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ExportFormat {
    JSON, CSV, TSV, TXT
}

data class ExportData(
    val tasks: List<TaskEntity> = emptyList(),
    val checklists: List<ChecklistWithItems> = emptyList()
)

data class ChecklistWithItems(
    val checklist: ChecklistEntity,
    val items: List<ObjectiveEntity>
)

class DataExportManager {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    fun exportData(data: ExportData, format: ExportFormat): String {
        return when (format) {
            ExportFormat.JSON -> gson.toJson(data)
            ExportFormat.CSV -> toCsv(data)
            ExportFormat.TSV -> toTsv(data)
            ExportFormat.TXT -> toTxt(data)
        }
    }

    private fun toCsv(data: ExportData): String {
        return toDelimiterSeparated(data, ",")
    }

    private fun toTsv(data: ExportData): String {
        // Same as CSV but with tabs
        val csv = toCsv(data)
        return csv.replace(",", "\t") 
        // Note: Simple replace is risky if content has commas. 
        // Better to reimplement with \t delimiter logic or ensure escapeCsv handles it.
        // For MVP, let's just reimplement correctly using a helper.
        return toDelimiterSeparated(data, "\t")
    }
    
    private fun toDelimiterSeparated(data: ExportData, delimiter: String): String {
         val sb = StringBuilder()
        
        if (data.tasks.isNotEmpty()) {
            sb.append("ID${delimiter}Name${delimiter}Description${delimiter}StartTime${delimiter}EndTime${delimiter}Repeatability${delimiter}IsFloating${delimiter}Color${delimiter}CustomRepeatDays${delimiter}Reminders\n")
            data.tasks.forEach { task ->
                sb.append(escape(task.id.toString(), delimiter)).append(delimiter)
                sb.append(escape(task.name, delimiter)).append(delimiter)
                sb.append(escape(task.description ?: "", delimiter)).append(delimiter)
                sb.append(task.startTime).append(delimiter)
                sb.append(task.endTime).append(delimiter)
                sb.append(escape(task.repeatability, delimiter)).append(delimiter)
                sb.append(task.isFloating).append(delimiter)
                sb.append(task.color).append(delimiter)
                sb.append(task.customRepeatDays ?: "").append(delimiter)
                // Reminders: List<Int> -> join with semicolon
                val remindersStr = task.reminders.joinToString(";")
                sb.append(escape(remindersStr, delimiter)).append("\n")
            }
        }

        if (data.checklists.isNotEmpty()) {
            if (sb.isNotEmpty()) sb.append("\n")
            sb.append("ChecklistID${delimiter}ChecklistName${delimiter}IsCompleted${delimiter}ItemID${delimiter}ItemText${delimiter}ItemCompleted\n")
            data.checklists.forEach { list ->
                if (list.items.isEmpty()) {
                    sb.append(escape(list.checklist.id, delimiter)).append(delimiter)
                    sb.append(escape(list.checklist.name, delimiter)).append(delimiter)
                    sb.append(list.checklist.isCompleted).append("$delimiter$delimiter$delimiter\n")
                } else {
                    list.items.forEach { item ->
                        sb.append(escape(list.checklist.id, delimiter)).append(delimiter)
                        sb.append(escape(list.checklist.name, delimiter)).append(delimiter)
                        sb.append(list.checklist.isCompleted).append(delimiter)
                        sb.append(escape(item.id, delimiter)).append(delimiter)
                        sb.append(escape(item.text, delimiter)).append(delimiter)
                        sb.append(item.isCompleted).append("\n")
                    }
                }
            }
        }
        return sb.toString()
    }

    private fun toTxt(data: ExportData): String {
        val sb = StringBuilder()
        val sdf = SimpleDateFormat("EEE, dd MMM HH:mm", Locale.getDefault())

        if (data.tasks.isNotEmpty()) {
            sb.append("=== TASKS ===\n\n")
            data.tasks.forEach { task ->
                sb.append("• ${task.name}\n")
                if (!task.description.isNullOrBlank()) {
                    sb.append("  Desc: ${task.description}\n")
                }
                val timeStr = if (task.isFloating) {
                    val sh = (task.startTime / 60).toInt()
                    val sm = (task.startTime % 60).toInt()
                    String.format("%02d:%02d", sh, sm)
                } else {
                    sdf.format(Date(task.startTime))
                }
                sb.append("  Time: $timeStr\n")
                sb.append("  Repeat: ${task.repeatability}\n\n")
            }
        }

        if (data.checklists.isNotEmpty()) {
            if (sb.isNotEmpty()) sb.append("\n")
            sb.append("=== CHECKLISTS ===\n\n")
            data.checklists.forEach { list ->
                val status = if (list.checklist.isCompleted) "[X]" else "[ ]"
                sb.append("$status ${list.checklist.name}\n")
                list.items.forEach { item ->
                    val itemStatus = if (item.isCompleted) "[x]" else "[ ]"
                    sb.append("   $itemStatus ${item.text}\n")
                }
                sb.append("\n")
            }
        }
        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        var result = value
        if (result.contains(",") || result.contains("\"") || result.contains("\n")) {
            result = result.replace("\"", "\"\"")
            result = "\"$result\""
        }
        return result
    }
    
    private fun escape(value: String, delimiter: String): String {
         var result = value
         // Basic escaping: if contains delimiter or newlines, quote it.
        if (result.contains(delimiter) || result.contains("\"") || result.contains("\n")) {
            result = result.replace("\"", "\"\"")
            result = "\"$result\""
        }
        return result
    }
}
