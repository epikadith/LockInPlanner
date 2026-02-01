package com.example.lockinplanner.domain.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.lockinplanner.data.local.entity.TaskEntity
import java.util.UUID

data class Task(
    val id: Int = 0,
    val name: String,
    val description: String? = null,
    val color: Color,
    val repeatability: String,
    val customRepeatDays: Int? = null,
    val isFloating: Boolean,
    val startTime: Long, 
    val endTime: Long,
    val reminders: List<Int> = emptyList(),
    
    // UI/Display Convenience Fields (Mapped during retrieval/projection)
    // These hold the "Display Time" values (e.g. 10:00 for a task shown at 10am).
    // For Fixed tasks, these are calculated relative to the Viewer's Timezone.
    // For Floating tasks, these are direct.
    val startHour: Int = 0,
    val startMinute: Int = 0,
    val endHour: Int = 0,
    val endMinute: Int = 0
)

fun Task.toEntity(): TaskEntity {
    return TaskEntity(
        id = id,
        name = name,
        description = description,
        color = color.value.toLong(), // ULong to Long
        repeatability = repeatability,
        customRepeatDays = customRepeatDays,
        isFloating = isFloating,
        startTime = startTime,
        endTime = endTime,
        reminders = reminders
    )
}

fun TaskEntity.toDomain(): Task {
    // When reading from DB, we don't know the Timezone yet.
    // So we populate startHour/Minute as "Raw UTC" or "Raw Mins".
    // UI layer must map this again using `.copy()` to project it to Visual Time.
    
    val sH: Int
    val sM: Int
    val eH: Int
    val eM: Int
    
    if (isFloating) {
        // Floating: startTime is minutes from midnight
        val s = startTime.toInt()
        sH = s / 60
        sM = s % 60
        val e = endTime.toInt()
        eH = e / 60
        eM = e % 60
    } else {
        // Fixed: startTime is UTC millis.
        // We default to UTC Calendar for the raw domain object.
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        cal.timeInMillis = startTime
        sH = cal.get(java.util.Calendar.HOUR_OF_DAY)
        sM = cal.get(java.util.Calendar.MINUTE)
        
        cal.timeInMillis = endTime
        eH = cal.get(java.util.Calendar.HOUR_OF_DAY)
        eM = cal.get(java.util.Calendar.MINUTE)
    }

    return Task(
        id = id,
        name = name,
        description = description,
        color = Color(color.toULong()), // Long to ULong to Color
        repeatability = repeatability,
        customRepeatDays = customRepeatDays,
        isFloating = isFloating,
        startTime = startTime,
        endTime = endTime,
        reminders = reminders,
        startHour = sH,
        startMinute = sM,
        endHour = eH,
        endMinute = eM
    )
}
