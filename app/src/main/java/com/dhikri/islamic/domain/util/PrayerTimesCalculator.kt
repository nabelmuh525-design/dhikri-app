package com.dhikri.islamic.domain.util

import java.util.Calendar
import java.util.TimeZone
import kotlin.math.*

enum class CalculationMethod(
    val key: String,
    val nameArabic: String,
    val fajrAngle: Double,
    val ishaAngle: Double,
    val ishaFixedMinutesAfterMaghrib: Int? = null,
    val noteArabic: String
) {
    JORDAN_AWQAF(
        key = "jordan_awqaf",
        nameArabic = "وزارة الأوقاف الأردنية (تقريبي)",
        fajrAngle = 18.0,
        ishaAngle = 18.0,
        noteArabic = "حساب فلكي بزوايا 18° للفجر والعشاء (قد يختلف 1-2 دقيقة عن التقويم الرسمي المطبوع)"
    ),
    UMM_AL_QURA(
        key = "umm_al_qura",
        nameArabic = "أم القرى (مكة المكرمة)",
        fajrAngle = 18.5,
        ishaAngle = 0.0,
        ishaFixedMinutesAfterMaghrib = 90,
        noteArabic = "زاوية الفجر 18.5° والعشاء بعد المغرب بـ 90 دقيقة"
    ),
    MWL(
        key = "mwl",
        nameArabic = "رابطة العالم الإسلامي",
        fajrAngle = 18.0,
        ishaAngle = 17.0,
        noteArabic = "زاوية الفجر 18° والعشاء 17°"
    ),
    EGYPT(
        key = "egypt",
        nameArabic = "الهيئة المصرية العامة للمساحة",
        fajrAngle = 19.5,
        ishaAngle = 17.5,
        noteArabic = "زاوية الفجر 19.5° والعشاء 17.5°"
    ),
    ISNA(
        key = "isna",
        nameArabic = "الجمعية الإسلامية لأمريكا الشمالية (ISNA)",
        fajrAngle = 15.0,
        ishaAngle = 15.0,
        noteArabic = "زاوية الفجر 15° والعشاء 15°"
    ),
    KARACHI(
        key = "karachi",
        nameArabic = "جامعة العلوم الإسلامية بكراتشي",
        fajrAngle = 18.0,
        ishaAngle = 18.0,
        noteArabic = "زاوية الفجر 18° والعشاء 18°"
    );

    companion object {
        fun fromKey(key: String?): CalculationMethod =
            entries.firstOrNull { it.key == key } ?: JORDAN_AWQAF
    }
}

enum class AsrMadhab(val key: String, val nameArabic: String, val shadowFactor: Double) {
    SHAFI("shafi", "الجمهور (الشافعي / المالكي / الحنبلي)", 1.0),
    HANAFI("hanafi", "المذهب الحنفي", 2.0);

    companion object {
        fun fromKey(key: String?): AsrMadhab =
            entries.firstOrNull { it.key == key } ?: SHAFI
    }
}

enum class HighLatitudeRule(val key: String, val nameArabic: String) {
    MIDDLE_OF_THE_NIGHT("middle_of_night", "منتصف الليل"),
    SEVENTH_OF_THE_NIGHT("seventh_of_night", "سُبع الليل"),
    ANGLE_BASED("angle_based", "الزاوية النسبية (1/60)");

    companion object {
        fun fromKey(key: String?): HighLatitudeRule =
            entries.firstOrNull { it.key == key } ?: ANGLE_BASED
    }
}

data class PrayerOffsets(
    val fajr: Int = 0,
    val dhuhr: Int = 0,
    val asr: Int = 0,
    val maghrib: Int = 0,
    val isha: Int = 0
)

data class PrayerCalculationConfig(
    val method: CalculationMethod = CalculationMethod.JORDAN_AWQAF,
    val asrMadhab: AsrMadhab = AsrMadhab.SHAFI,
    val highLatitudeRule: HighLatitudeRule = HighLatitudeRule.ANGLE_BASED,
    val offsets: PrayerOffsets = PrayerOffsets()
)

data class PrayerCity(
    val nameArabic: String,
    val latitude: Double,
    val longitude: Double,
    val timeZoneId: String,
    val defaultMethod: CalculationMethod = CalculationMethod.JORDAN_AWQAF
)

data class PrayerTimeItem(
    val nameArabic: String,
    val hour24: Int,
    val minute: Int,
    val timeMillis: Long
) {
    fun formatted12h(): String {
        val period = if (hour24 < 12) "ص" else "م"
        val h12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        return "%d:%02d %s".format(h12, minute, period)
    }
}

data class DailyPrayerSchedule(
    val city: PrayerCity,
    val prayers: List<PrayerTimeItem>,
    val nextPrayer: PrayerTimeItem,
    val millisRemaining: Long,
    val methodUsed: CalculationMethod = CalculationMethod.JORDAN_AWQAF,
    val asrMadhab: AsrMadhab = AsrMadhab.SHAFI,
    val isApproximateDisclaimer: Boolean = true
)

object PrayerTimesCalculator {

    val cities: List<PrayerCity> = listOf(
        PrayerCity("عمّان", 31.9539, 35.9106, "Asia/Amman", CalculationMethod.JORDAN_AWQAF),
        PrayerCity("إربد", 32.5568, 35.8469, "Asia/Amman", CalculationMethod.JORDAN_AWQAF),
        PrayerCity("الزرقاء", 32.0728, 36.0880, "Asia/Amman", CalculationMethod.JORDAN_AWQAF),
        PrayerCity("العقبة", 29.5321, 35.0063, "Asia/Amman", CalculationMethod.JORDAN_AWQAF),
        PrayerCity("القدس", 31.7683, 35.2137, "Asia/Jerusalem", CalculationMethod.JORDAN_AWQAF),
        PrayerCity("مكة المكرمة", 21.4225, 39.8262, "Asia/Riyadh", CalculationMethod.UMM_AL_QURA),
        PrayerCity("المدينة المنورة", 24.4672, 39.6112, "Asia/Riyadh", CalculationMethod.UMM_AL_QURA),
        PrayerCity("الرياض", 24.7136, 46.6753, "Asia/Riyadh", CalculationMethod.UMM_AL_QURA),
        PrayerCity("القاهرة", 30.0444, 31.2357, "Africa/Cairo", CalculationMethod.EGYPT),
        PrayerCity("دمشق", 33.5138, 36.2765, "Asia/Damascus", CalculationMethod.MWL),
        PrayerCity("بيروت", 33.8938, 35.5018, "Asia/Beirut", CalculationMethod.MWL),
        PrayerCity("بغداد", 33.3152, 44.3661, "Asia/Baghdad", CalculationMethod.MWL),
        PrayerCity("دبي", 25.2048, 55.2708, "Asia/Dubai", CalculationMethod.UMM_AL_QURA),
        PrayerCity("الدوحة", 25.2854, 51.5310, "Asia/Qatar", CalculationMethod.UMM_AL_QURA),
        PrayerCity("لندن", 51.5074, -0.1278, "Europe/London", CalculationMethod.MWL),
        PrayerCity("نيويورك", 40.7128, -74.0060, "America/New_York", CalculationMethod.ISNA),
        PrayerCity("أوسلو", 59.9139, 10.7522, "Europe/Oslo", CalculationMethod.MWL)
    )

    fun cityByName(name: String): PrayerCity =
        cities.firstOrNull { it.nameArabic == name } ?: cities.first()

    /**
     * Backward-compatible entry point using city's default method.
     */
    fun computeSchedule(
        cityName: String,
        nowMillis: Long = System.currentTimeMillis(),
        config: PrayerCalculationConfig? = null
    ): DailyPrayerSchedule {
        val city = cityByName(cityName)
        val effectiveConfig = config ?: PrayerCalculationConfig(method = city.defaultMethod)
        return computeScheduleForCity(city, nowMillis, effectiveConfig)
    }

    fun computeScheduleForCity(
        city: PrayerCity,
        nowMillis: Long = System.currentTimeMillis(),
        config: PrayerCalculationConfig = PrayerCalculationConfig(method = city.defaultMethod)
    ): DailyPrayerSchedule {
        val tz = TimeZone.getTimeZone(city.timeZoneId)
        val cal = Calendar.getInstance(tz).apply { timeInMillis = nowMillis }
        val todayPrayers = computeForDate(city, cal, config)

        val upcomingToday = todayPrayers.firstOrNull { it.timeMillis > nowMillis }
        val nextPrayer = if (upcomingToday != null) {
            upcomingToday
        } else {
            val tomorrow = Calendar.getInstance(tz).apply {
                timeInMillis = nowMillis
                add(Calendar.DAY_OF_YEAR, 1)
            }
            computeForDate(city, tomorrow, config).first()
        }

        val remaining = (nextPrayer.timeMillis - nowMillis).coerceAtLeast(0L)
        return DailyPrayerSchedule(
            city = city,
            prayers = todayPrayers,
            nextPrayer = nextPrayer,
            millisRemaining = remaining,
            methodUsed = config.method,
            asrMadhab = config.asrMadhab,
            isApproximateDisclaimer = true
        )
    }

    fun computeForDate(
        city: PrayerCity,
        dateCal: Calendar,
        config: PrayerCalculationConfig = PrayerCalculationConfig(method = city.defaultMethod)
    ): List<PrayerTimeItem> {
        val tz = TimeZone.getTimeZone(city.timeZoneId)
        val localCal = Calendar.getInstance(tz).apply {
            set(Calendar.YEAR, dateCal.get(Calendar.YEAR))
            set(Calendar.MONTH, dateCal.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, dateCal.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val year = localCal.get(Calendar.YEAR)
        val month = localCal.get(Calendar.MONTH) + 1
        val day = localCal.get(Calendar.DAY_OF_MONTH)
        // Use city's actual timezone offset (automatically handles DST for that exact date!)
        val tzHours = tz.getOffset(localCal.timeInMillis) / 3_600_000.0

        val jd = julianDate(year, month, day) - city.longitude / (15.0 * 24.0)
        val (decl, eqTime) = sunPosition(jd + 0.5)

        val dhuhr = fixHour(12.0 + tzHours - city.longitude / 15.0 - eqTime)

        // Safe sunrise/sunset with polar fallback (clamped to 6:00 / 18:00 solar if sun never sets/rises)
        val sunriseAngle = hourAngleOrNull(0.833, city.latitude, decl)
        val sunrise = if (sunriseAngle != null) dhuhr - sunriseAngle else dhuhr - 6.0
        val maghrib = if (sunriseAngle != null) dhuhr + sunriseAngle else dhuhr + 6.0

        val asrDelta = asrTimeOffsetOrNull(config.asrMadhab.shadowFactor, city.latitude, decl)
            ?: ((maghrib - dhuhr) * 0.55)
        val asr = dhuhr + asrDelta

        val nightDuration = fixHour(sunrise - maghrib).let { if (it <= 0.0 || it >= 24.0) 8.0 else it }

        val rawFajrAngle = hourAngleOrNull(config.method.fajrAngle, city.latitude, decl)
        var fajr = if (rawFajrAngle != null) {
            dhuhr - rawFajrAngle
        } else {
            sunrise - nightPortion(config.method.fajrAngle, config.highLatitudeRule) * nightDuration
        }

        var isha = if (config.method.ishaFixedMinutesAfterMaghrib != null) {
            maghrib + (config.method.ishaFixedMinutesAfterMaghrib / 60.0)
        } else {
            val rawIshaAngle = hourAngleOrNull(config.method.ishaAngle, city.latitude, decl)
            if (rawIshaAngle != null) {
                dhuhr + rawIshaAngle
            } else {
                maghrib + nightPortion(config.method.ishaAngle, config.highLatitudeRule) * nightDuration
            }
        }

        // High-latitude safety clamp (for latitudes >= 48° where twilight can become abnormal)
        if (abs(city.latitude) >= 48.0) {
            val maxFajrDiff = nightPortion(config.method.fajrAngle, config.highLatitudeRule) * nightDuration
            if (sunrise - fajr > maxFajrDiff || fajr < 0.0) {
                fajr = sunrise - maxFajrDiff
            }
            if (config.method.ishaFixedMinutesAfterMaghrib == null) {
                val maxIshaDiff = nightPortion(config.method.ishaAngle, config.highLatitudeRule) * nightDuration
                if (isha - maghrib > maxIshaDiff || isha > 24.0) {
                    isha = maghrib + maxIshaDiff
                }
            }
        }

        // Apply manual per-prayer minute offsets
        fajr += config.offsets.fajr / 60.0
        val dhuhrAdj = dhuhr + config.offsets.dhuhr / 60.0
        val asrAdj = asr + config.offsets.asr / 60.0
        val maghribAdj = maghrib + config.offsets.maghrib / 60.0
        isha += config.offsets.isha / 60.0

        // Guarantee monotonic logical ordering (prevents impossible times at polar extremes)
        val safeFajr = fajr.coerceIn(0.0, dhuhrAdj - 0.5)
        val safeAsr = asrAdj.coerceIn(dhuhrAdj + 0.5, maghribAdj - 0.25)
        val safeMaghrib = maghribAdj.coerceAtLeast(safeAsr + 0.25)
        val safeIsha = isha.coerceAtLeast(safeMaghrib + 0.25)

        return listOf(
            buildItem("الفجر", safeFajr, localCal),
            buildItem("الظهر", dhuhrAdj, localCal),
            buildItem("العصر", safeAsr, localCal),
            buildItem("المغرب", safeMaghrib, localCal),
            buildItem("العشاء", safeIsha, localCal)
        )
    }

    private fun nightPortion(angle: Double, rule: HighLatitudeRule): Double = when (rule) {
        HighLatitudeRule.MIDDLE_OF_THE_NIGHT -> 0.5
        HighLatitudeRule.SEVENTH_OF_THE_NIGHT -> 1.0 / 7.0
        HighLatitudeRule.ANGLE_BASED -> angle / 60.0
    }

    private fun buildItem(name: String, decimalHours: Double, baseCal: Calendar): PrayerTimeItem {
        // Uniform nearest-minute rounding with clean midnight-crossing day rollover
        val totalMinutes = floor(decimalHours * 60.0 + 0.5).toInt()
        val dayOffset = floor(totalMinutes / 1440.0).toInt()
        val minuteOfDay = ((totalMinutes % 1440) + 1440) % 1440
        val h = minuteOfDay / 60
        val m = minuteOfDay % 60

        val itemCal = (baseCal.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, dayOffset)
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, m)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return PrayerTimeItem(name, h, m, itemCal.timeInMillis)
    }

    private fun julianDate(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2.0 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private fun sunPosition(jd: Double): Pair<Double, Double> {
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * sinDeg(g) + 0.020 * sinDeg(2.0 * g))
        val e = 23.439 - 0.00000036 * d
        val ra = atan2Deg(cosDeg(e) * sinDeg(l), cosDeg(l)) / 15.0
        val eqTime = q / 15.0 - fixHour(ra)
        val decl = asinDeg(sinDeg(e) * sinDeg(l))
        return decl to eqTime
    }

    private fun hourAngleOrNull(angle: Double, lat: Double, decl: Double): Double? {
        val denom = cosDeg(lat) * cosDeg(decl)
        if (abs(denom) < 1e-9) return null
        val cosH = (-sinDeg(angle) - sinDeg(lat) * sinDeg(decl)) / denom
        if (cosH < -1.0 || cosH > 1.0) return null
        return acosDeg(cosH) / 15.0
    }

    private fun asrTimeOffsetOrNull(factor: Double, lat: Double, decl: Double): Double? {
        val angle = -atanDeg(1.0 / (factor + tanDeg(abs(lat - decl))))
        return hourAngleOrNull(angle, lat, decl)
    }

    private fun sinDeg(d: Double) = sin(Math.toRadians(d))
    private fun cosDeg(d: Double) = cos(Math.toRadians(d))
    private fun tanDeg(d: Double) = tan(Math.toRadians(d))
    private fun asinDeg(x: Double) = Math.toDegrees(asin(x.coerceIn(-1.0, 1.0)))
    private fun acosDeg(x: Double) = Math.toDegrees(acos(x.coerceIn(-1.0, 1.0)))
    private fun atanDeg(x: Double) = Math.toDegrees(atan(x))
    private fun atan2Deg(y: Double, x: Double) = Math.toDegrees(atan2(y, x))
    private fun fixAngle(a: Double): Double {
        var r = a % 360.0
        if (r < 0) r += 360.0
        return r
    }

    private fun fixHour(h: Double): Double {
        var r = h % 24.0
        if (r < 0) r += 24.0
        return r
    }

    fun formatCountdown(millis: Long): String {
        val totalSec = (millis / 1000L).coerceAtLeast(0L)
        val h = totalSec / 3600L
        val m = (totalSec % 3600L) / 60L
        val s = totalSec % 60L
        return "%02d:%02d:%02d".format(h, m, s)
    }
}
