package com.example.lockinplanner.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.lockinplanner.domain.model.AppTheme
import com.example.lockinplanner.domain.model.TimezoneProfile
import com.example.lockinplanner.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class UserPreferencesRepository(private val context: Context) {
    
    private object PreferencesKeys {
        val THEME = stringPreferencesKey("app_theme")
        val DATE_FORMAT = stringPreferencesKey("date_format")
        val TIME_FORMAT_24H = booleanPreferencesKey("time_format_24h")
        val CONFIRM_DELETION = booleanPreferencesKey("confirm_deletion")
        val TIMEZONE_ENABLED = booleanPreferencesKey("timezone_enabled")
        val MAIN_TIMEZONE = stringPreferencesKey("main_timezone")
        val SELECTED_PROFILE_ID = stringPreferencesKey("selected_profile_id")
        val TIMEZONE_PROFILES = stringPreferencesKey("timezone_profiles")
        
        // Notifications
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val NOTIFY_DAILY = booleanPreferencesKey("notify_daily")
        val NOTIFY_CUSTOM = booleanPreferencesKey("notify_custom")
        val NOTIFY_SINGLE = booleanPreferencesKey("notify_single")

        // Haptics
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")

        // Undo Deletion
        val UNDO_ENABLED = booleanPreferencesKey("undo_enabled")
        val UNDO_DURATION = androidx.datastore.preferences.core.intPreferencesKey("undo_duration")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .map { preferences ->
            val themeName = preferences[PreferencesKeys.THEME] ?: AppTheme.System.name
            val theme = try {
                AppTheme.valueOf(themeName)
            } catch (e: IllegalArgumentException) {
                AppTheme.System
            }

            val profilesJson = preferences[PreferencesKeys.TIMEZONE_PROFILES] ?: "[]"
            val profiles = try {
                val jsonArray = org.json.JSONArray(profilesJson)
                val list = mutableListOf<TimezoneProfile>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    list.add(TimezoneProfile(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        timezoneId = obj.getString("timezoneId")
                    ))
                }
                list
            } catch (e: Exception) {
                emptyList()
            }

            UserPreferences(
                theme = theme,
                dateFormat = preferences[PreferencesKeys.DATE_FORMAT] ?: "dd/MM/yyyy",
                timeFormat24h = preferences[PreferencesKeys.TIME_FORMAT_24H] ?: true,
                confirmDeletion = preferences[PreferencesKeys.CONFIRM_DELETION] ?: true,
                timezoneEnabled = preferences[PreferencesKeys.TIMEZONE_ENABLED] ?: false,
                mainTimezone = preferences[PreferencesKeys.MAIN_TIMEZONE] ?: java.util.TimeZone.getDefault().id,
                selectedProfileId = preferences[PreferencesKeys.SELECTED_PROFILE_ID],
                timezoneProfiles = profiles,
                notificationsEnabled = preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true,
                notifyDaily = preferences[PreferencesKeys.NOTIFY_DAILY] ?: true,
                notifyCustom = preferences[PreferencesKeys.NOTIFY_CUSTOM] ?: true,
                notifySingle = preferences[PreferencesKeys.NOTIFY_SINGLE] ?: true,
                hapticsEnabled = preferences[PreferencesKeys.HAPTICS_ENABLED] ?: true,
                undoEnabled = preferences[PreferencesKeys.UNDO_ENABLED] ?: true,
                undoDuration = preferences[PreferencesKeys.UNDO_DURATION] ?: 5
            )
        }

    suspend fun updateTheme(theme: AppTheme) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME] = theme.name
        }
    }

    suspend fun updateDateFormat(format: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DATE_FORMAT] = format
        }
    }

    suspend fun updateTimeFormat(is24h: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TIME_FORMAT_24H] = is24h
        }
    }
    
    suspend fun updateConfirmDeletion(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CONFIRM_DELETION] = enabled
        }
    }

    suspend fun updateTimezoneEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TIMEZONE_ENABLED] = enabled
        }
    }
    
    suspend fun updateMainTimezone(timezoneId: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.MAIN_TIMEZONE] = timezoneId
        }
    }

    suspend fun updateSelectedProfileId(profileId: String?) {
        context.dataStore.edit { preferences ->
            if (profileId != null) {
                preferences[PreferencesKeys.SELECTED_PROFILE_ID] = profileId
            } else {
                preferences.remove(PreferencesKeys.SELECTED_PROFILE_ID)
            }
        }
    }

    suspend fun updateTimezoneProfiles(profiles: List<TimezoneProfile>) {
        context.dataStore.edit { preferences ->
            val jsonArray = org.json.JSONArray()
            profiles.forEach { profile ->
                val obj = org.json.JSONObject()
                obj.put("id", profile.id)
                obj.put("name", profile.name)
                obj.put("timezoneId", profile.timezoneId)
                jsonArray.put(obj)
            }
            preferences[PreferencesKeys.TIMEZONE_PROFILES] = jsonArray.toString()
        }
    }

    suspend fun updateNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun updateNotifyDaily(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFY_DAILY] = enabled
        }
    }

    suspend fun updateNotifyCustom(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFY_CUSTOM] = enabled
        }
    }

    suspend fun updateNotifySingle(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFY_SINGLE] = enabled
        }
    }

    suspend fun updateHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAPTICS_ENABLED] = enabled
        }
    }

    suspend fun updateUndoEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.UNDO_ENABLED] = enabled
        }
    }

    suspend fun updateUndoDuration(duration: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.UNDO_DURATION] = duration
        }
    }
}
