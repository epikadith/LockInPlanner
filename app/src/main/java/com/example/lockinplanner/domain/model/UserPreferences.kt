package com.example.lockinplanner.domain.model

data class UserPreferences(
    val theme: AppTheme = AppTheme.System,
    val dateFormat: String = "dd/MM/yyyy",
    val timeFormat24h: Boolean = true,
    val confirmDeletion: Boolean = true,
    val timezoneEnabled: Boolean = false,
    val mainTimezone: String = java.util.TimeZone.getDefault().id,
    val selectedProfileId: String? = null,
    val timezoneProfiles: List<TimezoneProfile> = emptyList(),
    // Notifications
    val notificationsEnabled: Boolean = true,
    val notifyDaily: Boolean = true,
    val notifyCustom: Boolean = true,
    val notifySingle: Boolean = true,
    // Haptics
    val hapticsEnabled: Boolean = true,
    // Undo Deletion
    val undoEnabled: Boolean = true,
    val undoDuration: Int = 5,
    // Notes Display Preferences
    val shortsDisplayMode: Int = 0,
    val bookViewColumnCount: Int = 2,
    val chapterViewColumnCount: Int = 2,
    // Custom Theme
    val customPrimaryColor: Long? = null,
    val customSecondaryColor: Long? = null,
    val customIsDarkBackground: Boolean = true
)
