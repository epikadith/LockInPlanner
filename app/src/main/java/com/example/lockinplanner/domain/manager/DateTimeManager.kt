package com.example.lockinplanner.domain.manager

import com.example.lockinplanner.domain.model.UserPreferences
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

object DateTimeManager {

    fun getDisplayTimeZone(userPreferences: UserPreferences): TimeZone {
        return if (userPreferences.timezoneEnabled) {
            val targetId = if (userPreferences.selectedProfileId != null) {
                userPreferences.timezoneProfiles.find { it.id == userPreferences.selectedProfileId }?.timezoneId
                    ?: userPreferences.mainTimezone
            } else {
                userPreferences.mainTimezone
            }
            TimeZone.getTimeZone(targetId)
        } else {
            TimeZone.getDefault()
        }
    }

    fun getCalendar(userPreferences: UserPreferences): Calendar {
        return Calendar.getInstance(getDisplayTimeZone(userPreferences))
    }

    fun formatTime(
        hour: Int, 
        minute: Int, 
        userPreferences: UserPreferences
    ): String {
        val is24h = userPreferences.timeFormat24h
        return if (is24h) {
            String.format(Locale.US, "%02d:%02d", hour, minute)
        } else {
            val amPm = if (hour < 12) "AM" else "PM"
            val h = if (hour == 0 || hour == 12) 12 else hour % 12
            String.format(Locale.US, "%d:%02d %s", h, minute, amPm)
        }
    }

    // Helper to calculate Absolute Start Time of a task
    // Assumes task.date is the UTC timestamp of the "Day Start" in the creation timezone
    // But since we don't store creation timezone, we treat task.date as the 00:00 basis.
    fun getTaskAbsoluteStart(taskDate: Long, startHour: Int, startMinute: Int): Long {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.timeInMillis = taskDate
        cal.add(Calendar.HOUR_OF_DAY, startHour)
        cal.add(Calendar.MINUTE, startMinute)
        return cal.timeInMillis
    }
}
