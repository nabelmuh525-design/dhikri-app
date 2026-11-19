package com.dhikri.islamic.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "dhikri_user_settings")

data class UserSettings(
    val darkMode: Boolean = false,
    val nightReadingMode: Boolean = false,
    val readingThemeKey: String = "parchment",
    val mushafFontSizeSp: Float = 26f,
    val uiFontScale: Float = 1.0f,
    val vibrationEnabled: Boolean = true,
    val soundEnabled: Boolean = false,
    val notificationsEnabled: Boolean = false,
    val lastTasbihPresetId: Int = 1,
    val useHafsV20Text: Boolean = true,
    val khatmahDays: Int = 30,
    val khatmahCurrentPage: Int = 1,
    val khatmahActive: Boolean = false,
    val dailyTasbihGoal: Int = 100
)

class UserSettingsRepository(private val context: Context) {

    private object Keys {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val NIGHT_READING = booleanPreferencesKey("night_reading")
        val READING_THEME_KEY = stringPreferencesKey("reading_theme_key")
        val MUSHAF_FONT_SIZE = floatPreferencesKey("mushaf_font_size_sp")
        val UI_FONT_SCALE = floatPreferencesKey("ui_font_scale")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val LAST_TASBIH_PRESET_ID = intPreferencesKey("last_tasbih_preset_id")
        val USE_HAFS_V20_TEXT = booleanPreferencesKey("use_hafs_v20_text")
        val KHATMAH_DAYS = intPreferencesKey("khatmah_days")
        val KHATMAH_CURRENT_PAGE = intPreferencesKey("khatmah_current_page")
        val KHATMAH_ACTIVE = booleanPreferencesKey("khatmah_active")
        val DAILY_TASBIH_GOAL = intPreferencesKey("daily_tasbih_goal")
    }

    val settings: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        val nightLegacy = prefs[Keys.NIGHT_READING] ?: false
        val savedTheme = prefs[Keys.READING_THEME_KEY] ?: if (nightLegacy) "amoled_dark" else "parchment"
        UserSettings(
            darkMode = prefs[Keys.DARK_MODE] ?: false,
            nightReadingMode = nightLegacy,
            readingThemeKey = savedTheme,
            mushafFontSizeSp = prefs[Keys.MUSHAF_FONT_SIZE] ?: 26f,
            uiFontScale = prefs[Keys.UI_FONT_SCALE] ?: 1.0f,
            vibrationEnabled = prefs[Keys.VIBRATION_ENABLED] ?: true,
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: false,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: false,
            lastTasbihPresetId = prefs[Keys.LAST_TASBIH_PRESET_ID] ?: 1,
            useHafsV20Text = prefs[Keys.USE_HAFS_V20_TEXT] ?: true,
            khatmahDays = prefs[Keys.KHATMAH_DAYS] ?: 30,
            khatmahCurrentPage = prefs[Keys.KHATMAH_CURRENT_PAGE] ?: 1,
            khatmahActive = prefs[Keys.KHATMAH_ACTIVE] ?: false,
            dailyTasbihGoal = prefs[Keys.DAILY_TASBIH_GOAL] ?: 100
        )
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DARK_MODE] = enabled }
    }

    suspend fun setNightReadingMode(enabled: Boolean) {
        context.dataStore.edit {
            it[Keys.NIGHT_READING] = enabled
            it[Keys.READING_THEME_KEY] = if (enabled) "amoled_dark" else "parchment"
        }
    }

    suspend fun setReadingTheme(themeKey: String) {
        context.dataStore.edit {
            it[Keys.READING_THEME_KEY] = themeKey
            it[Keys.NIGHT_READING] = (themeKey == "amoled_dark" || themeKey == "sepia_dark")
        }
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

    suspend fun startKhatmah(days: Int, startPage: Int = 1) {
        context.dataStore.edit {
            it[Keys.KHATMAH_DAYS] = days.coerceIn(7, 365)
            it[Keys.KHATMAH_CURRENT_PAGE] = startPage.coerceIn(1, 604)
            it[Keys.KHATMAH_ACTIVE] = true
        }
    }

    suspend fun updateKhatmahPage(page: Int) {
        context.dataStore.edit {
            it[Keys.KHATMAH_CURRENT_PAGE] = page.coerceIn(1, 604)
            it[Keys.KHATMAH_ACTIVE] = true
        }
    }

    suspend fun setDailyTasbihGoal(goal: Int) {
        context.dataStore.edit { it[Keys.DAILY_TASBIH_GOAL] = goal.coerceIn(10, 10_000) }
    }

    suspend fun resetKhatmah() {
        context.dataStore.edit {
            it[Keys.KHATMAH_CURRENT_PAGE] = 1
            it[Keys.KHATMAH_ACTIVE] = false
        }
    }
}
