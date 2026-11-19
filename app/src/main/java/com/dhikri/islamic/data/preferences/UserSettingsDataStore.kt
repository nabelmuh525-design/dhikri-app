package com.dhikri.islamic.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "dhikri_user_settings")

data class UserSettings(
    val darkMode: Boolean = false,
    val nightReadingMode: Boolean = false,
    val mushafFontSizeSp: Float = 26f,
    val uiFontScale: Float = 1.0f,
    val vibrationEnabled: Boolean = true,
    val soundEnabled: Boolean = false,
    val notificationsEnabled: Boolean = false,
    val lastTasbihPresetId: Int = 1,
    val useHafsV20Text: Boolean = true
)

class UserSettingsRepository(private val context: Context) {

    private object Keys {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val NIGHT_READING = booleanPreferencesKey("night_reading")
        val MUSHAF_FONT_SIZE = floatPreferencesKey("mushaf_font_size_sp")
        val UI_FONT_SCALE = floatPreferencesKey("ui_font_scale")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val LAST_TASBIH_PRESET_ID = intPreferencesKey("last_tasbih_preset_id")
        val USE_HAFS_V20_TEXT = booleanPreferencesKey("use_hafs_v20_text")
    }

    val settings: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            darkMode = prefs[Keys.DARK_MODE] ?: false,
            nightReadingMode = prefs[Keys.NIGHT_READING] ?: false,
            mushafFontSizeSp = prefs[Keys.MUSHAF_FONT_SIZE] ?: 26f,
            uiFontScale = prefs[Keys.UI_FONT_SCALE] ?: 1.0f,
            vibrationEnabled = prefs[Keys.VIBRATION_ENABLED] ?: true,
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: false,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: false,
            lastTasbihPresetId = prefs[Keys.LAST_TASBIH_PRESET_ID] ?: 1,
            useHafsV20Text = prefs[Keys.USE_HAFS_V20_TEXT] ?: true
        )
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DARK_MODE] = enabled }
    }

    suspend fun setNightReadingMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NIGHT_READING] = enabled }
    }

    suspend fun setMushafFontSize(sizeSp: Float) {
        context.dataStore.edit { it[Keys.MUSHAF_FONT_SIZE] = sizeSp.coerceIn(18f, 42f) }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.VIBRATION_ENABLED] = enabled }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SOUND_ENABLED] = enabled }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setLastTasbihPresetId(id: Int) {
        context.dataStore.edit { it[Keys.LAST_TASBIH_PRESET_ID] = id }
    }
}
