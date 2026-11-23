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
    val themeMode: String = "system", // "system", "light", "dark"
    val nightReadingMode: Boolean = false,
    val readingThemeKey: String = "parchment",
    val mushafFontSizeSp: Float = 26f,
    val uiFontScale: Float = 1.0f,
    val vibrationEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val lastTasbihPresetId: Int = 1,
    val useHafsV20Text: Boolean = true,
    val khatmahDays: Int = 30,
    val khatmahCurrentPage: Int = 1,
    val khatmahActive: Boolean = false,
    val dailyTasbihGoal: Int = 100,
    val prayerCityName: String = "عمّان",
    val preAdhanReminderEnabled: Boolean = true,
    // v4.0.0 Prayer Calculation & Per-Prayer Notification Controls
    val calculationMethodKey: String = "jordan_awqaf",
    val asrMadhabKey: String = "shafi",
    val highLatitudeRuleKey: String = "angle_based",
    val offsetFajrMin: Int = 0,
    val offsetDhuhrMin: Int = 0,
    val offsetAsrMin: Int = 0,
    val offsetMaghribMin: Int = 0,
    val offsetIshaMin: Int = 0,
    val notifyFajr: Boolean = true,
    val notifyDhuhr: Boolean = true,
    val notifyAsr: Boolean = true,
    val notifyMaghrib: Boolean = true,
    val notifyIsha: Boolean = true,
    val postPrayerIstighfarEnabled: Boolean = true,
    val morningEveningAdhkarReminderEnabled: Boolean = true
)

class UserSettingsRepository(private val context: Context) {

    private object Keys {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val THEME_MODE = stringPreferencesKey("theme_mode")
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
        val PRAYER_CITY_NAME = stringPreferencesKey("prayer_city_name")
        val PRE_ADHAN_REMINDER = booleanPreferencesKey("pre_adhan_reminder")
        val CALCULATION_METHOD = stringPreferencesKey("calculation_method_key")
        val ASR_MADHAB = stringPreferencesKey("asr_madhab_key")
        val HIGH_LAT_RULE = stringPreferencesKey("high_latitude_rule_key")
        val OFFSET_FAJR = intPreferencesKey("offset_fajr_min")
        val OFFSET_DHUHR = intPreferencesKey("offset_dhuhr_min")
        val OFFSET_ASR = intPreferencesKey("offset_asr_min")
        val OFFSET_MAGHRIB = intPreferencesKey("offset_maghrib_min")
        val OFFSET_ISHA = intPreferencesKey("offset_isha_min")
        val NOTIFY_FAJR = booleanPreferencesKey("notify_fajr")
        val NOTIFY_DHUHR = booleanPreferencesKey("notify_dhuhr")
        val NOTIFY_ASR = booleanPreferencesKey("notify_asr")
        val NOTIFY_MAGHRIB = booleanPreferencesKey("notify_maghrib")
        val NOTIFY_ISHA = booleanPreferencesKey("notify_isha")
        val POST_PRAYER_ISTIGHFAR = booleanPreferencesKey("post_prayer_istighfar_enabled")
        val MORNING_EVENING_ADHKAR = booleanPreferencesKey("morning_evening_adhkar_reminder")
    }

    val settings: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        val nightLegacy = prefs[Keys.NIGHT_READING] ?: false
        val savedTheme = prefs[Keys.READING_THEME_KEY] ?: if (nightLegacy) "amoled_dark" else "parchment"
        val legacyDark = prefs[Keys.DARK_MODE] ?: false
        val savedThemeMode = prefs[Keys.THEME_MODE] ?: if (prefs.contains(Keys.DARK_MODE)) {
            if (legacyDark) "dark" else "light"
        } else {
            "system"
        }
        val unifiedNotif = prefs[Keys.PRE_ADHAN_REMINDER] ?: prefs[Keys.NOTIFICATIONS_ENABLED] ?: true
        UserSettings(
            darkMode = legacyDark,
            themeMode = savedThemeMode,
            nightReadingMode = nightLegacy,
            readingThemeKey = savedTheme,
            mushafFontSizeSp = prefs[Keys.MUSHAF_FONT_SIZE] ?: 26f,
            uiFontScale = prefs[Keys.UI_FONT_SCALE] ?: 1.0f,
            vibrationEnabled = prefs[Keys.VIBRATION_ENABLED] ?: true,
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
            notificationsEnabled = unifiedNotif,
            lastTasbihPresetId = prefs[Keys.LAST_TASBIH_PRESET_ID] ?: 1,
            useHafsV20Text = prefs[Keys.USE_HAFS_V20_TEXT] ?: true,
            khatmahDays = prefs[Keys.KHATMAH_DAYS] ?: 30,
            khatmahCurrentPage = prefs[Keys.KHATMAH_CURRENT_PAGE] ?: 1,
            khatmahActive = prefs[Keys.KHATMAH_ACTIVE] ?: false,
            dailyTasbihGoal = prefs[Keys.DAILY_TASBIH_GOAL] ?: 100,
            prayerCityName = prefs[Keys.PRAYER_CITY_NAME] ?: "عمّان",
            preAdhanReminderEnabled = unifiedNotif,
            calculationMethodKey = prefs[Keys.CALCULATION_METHOD] ?: "jordan_awqaf",
            asrMadhabKey = prefs[Keys.ASR_MADHAB] ?: "shafi",
            highLatitudeRuleKey = prefs[Keys.HIGH_LAT_RULE] ?: "angle_based",
            offsetFajrMin = prefs[Keys.OFFSET_FAJR] ?: 0,
            offsetDhuhrMin = prefs[Keys.OFFSET_DHUHR] ?: 0,
            offsetAsrMin = prefs[Keys.OFFSET_ASR] ?: 0,
            offsetMaghribMin = prefs[Keys.OFFSET_MAGHRIB] ?: 0,
            offsetIshaMin = prefs[Keys.OFFSET_ISHA] ?: 0,
            notifyFajr = prefs[Keys.NOTIFY_FAJR] ?: true,
            notifyDhuhr = prefs[Keys.NOTIFY_DHUHR] ?: true,
            notifyAsr = prefs[Keys.NOTIFY_ASR] ?: true,
            notifyMaghrib = prefs[Keys.NOTIFY_MAGHRIB] ?: true,
            notifyIsha = prefs[Keys.NOTIFY_ISHA] ?: true,
            postPrayerIstighfarEnabled = prefs[Keys.POST_PRAYER_ISTIGHFAR] ?: true,
            morningEveningAdhkarReminderEnabled = prefs[Keys.MORNING_EVENING_ADHKAR] ?: true
        )
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit {
            it[Keys.DARK_MODE] = enabled
            it[Keys.THEME_MODE] = if (enabled) "dark" else "light"
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit {
            it[Keys.THEME_MODE] = mode
            if (mode == "dark") it[Keys.DARK_MODE] = true
            if (mode == "light") it[Keys.DARK_MODE] = false
        }
    }

    suspend fun setPrayerCityName(cityName: String) {
        context.dataStore.edit { it[Keys.PRAYER_CITY_NAME] = cityName }
    }

    suspend fun setPreAdhanReminderEnabled(enabled: Boolean) {
        context.dataStore.edit {
            it[Keys.PRE_ADHAN_REMINDER] = enabled
            it[Keys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun setCalculationMethod(methodKey: String) {
        context.dataStore.edit { it[Keys.CALCULATION_METHOD] = methodKey }
    }

    suspend fun setAsrMadhab(madhabKey: String) {
        context.dataStore.edit { it[Keys.ASR_MADHAB] = madhabKey }
    }

    suspend fun setHighLatitudeRule(ruleKey: String) {
        context.dataStore.edit { it[Keys.HIGH_LAT_RULE] = ruleKey }
    }

    suspend fun setPrayerOffsets(fajr: Int, dhuhr: Int, asr: Int, maghrib: Int, isha: Int) {
        context.dataStore.edit {
            it[Keys.OFFSET_FAJR] = fajr.coerceIn(-30, 30)
            it[Keys.OFFSET_DHUHR] = dhuhr.coerceIn(-30, 30)
            it[Keys.OFFSET_ASR] = asr.coerceIn(-30, 30)
            it[Keys.OFFSET_MAGHRIB] = maghrib.coerceIn(-30, 30)
            it[Keys.OFFSET_ISHA] = isha.coerceIn(-30, 30)
        }
    }

    suspend fun setPrayerNotificationEnabled(prayerName: String, enabled: Boolean) {
        context.dataStore.edit {
            when (prayerName) {
                "الفجر" -> it[Keys.NOTIFY_FAJR] = enabled
                "الظهر" -> it[Keys.NOTIFY_DHUHR] = enabled
                "العصر" -> it[Keys.NOTIFY_ASR] = enabled
                "المغرب" -> it[Keys.NOTIFY_MAGHRIB] = enabled
                "العشاء" -> it[Keys.NOTIFY_ISHA] = enabled
            }
        }
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
        setPreAdhanReminderEnabled(enabled)
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
