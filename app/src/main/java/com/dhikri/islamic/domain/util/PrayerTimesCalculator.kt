package com.dhikri.islamic.domain.util

import java.util.Calendar
import java.util.TimeZone
import kotlin.math.*

data class PrayerCity(
    val nameArabic: String,
    val latitude: Double,
    val longitude: Double,
    val fajrAngle: Double = 18.0,
    val ishaAngle: Double = 18.0
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
    val millisRemaining: Long
)

object PrayerTimesCalculator {

    val cities: List<PrayerCity> = listOf(
        PrayerCity("عمّان", 31.9539, 35.9106, 18.0, 18.0),
        PrayerCity("إربد", 32.5568, 35.8469, 18.0, 18.0),
        PrayerCity("الزرقاء", 32.0728, 36.0880, 18.0, 18.0),
        PrayerCity("العقبة", 29.5321, 35.0063, 18.0, 18.0),
        PrayerCity("القدس", 31.7683, 35.2137, 18.0, 18.0),
        PrayerCity("مكة المكرمة", 21.4225, 39.8262, 18.5, 18.5),
        PrayerCity("المدينة المنورة", 24.4672, 39.6112, 18.5, 18.5),
        PrayerCity("الرياض", 24.7136, 46.6753, 18.5, 18.5),
        PrayerCity("القاهرة", 30.0444, 31.2357, 19.5, 17.5),
        PrayerCity("دمشق", 33.5138, 36.2765, 18.0, 18.0),
        PrayerCity("بيروت", 33.8938, 35.5018, 18.0, 18.0),
        PrayerCity("بغداد", 33.3152, 44.3661, 18.0, 18.0),
        PrayerCity("دبي", 25.2048, 55.2708, 18.2, 18.2),
        PrayerCity("الدوحة", 25.2854, 51.5310, 18.5, 18.5)
    )

    fun cityByName(name: String): PrayerCity =
        cities.firstOrNull { it.nameArabic == name } ?: cities.first()

    fun computeSchedule(cityName: String, nowMillis: Long = System.currentTimeMillis()): DailyPrayerSchedule {
        val city = cityByName(cityName)
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val todayPrayers = computeForDate(city, cal)

        val upcomingToday = todayPrayers.firstOrNull { it.timeMillis > nowMillis }
        val nextPrayer = if (upcomingToday != null) {
            upcomingToday
        } else {
            val tomorrow = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                add(Calendar.DAY_OF_YEAR, 1)
            }
            computeForDate(city, tomorrow).first()
        }

        val remaining = (nextPrayer.timeMillis - nowMillis).coerceAtLeast(0L)
        return DailyPrayerSchedule(
            city = city,
            prayers = todayPrayers,
            nextPrayer = nextPrayer,
            millisRemaining = remaining
        )
    }

    private fun computeForDate(city: PrayerCity, dateCal: Calendar): List<PrayerTimeItem> {
        val year = dateCal.get(Calendar.YEAR)
        val month = dateCal.get(Calendar.MONTH) + 1
        val day = dateCal.get(Calendar.DAY_OF_MONTH)
        val tzHours = TimeZone.getDefault().getOffset(dateCal.timeInMillis) / 3_600_000.0

        val jd = julianDate(year, month, day) - city.longitude / (15.0 * 24.0)
        val (decl, eqTime) = sunPosition(jd + 0.5)

        val dhuhr = fixHour(12.0 + tzHours - city.longitude / 15.0 - eqTime)
        val fajr = dhuhr - hourAngle(city.fajrAngle, city.latitude, decl)
        val maghrib = dhuhr + hourAngle(0.833, city.latitude, decl)
        val isha = dhuhr + hourAngle(city.ishaAngle, city.latitude, decl)
        val asr = dhuhr + asrTimeOffset(1.0, city.latitude, decl)

        return listOf(
            buildItem("الفجر", fajr, dateCal),
            buildItem("الظهر", dhuhr, dateCal),
            buildItem("العصر", asr, dateCal),
            buildItem("المغرب", maghrib, dateCal),
            buildItem("العشاء", isha, dateCal)
        )
    }

    private fun buildItem(name: String, decimalHours: Double, baseCal: Calendar): PrayerTimeItem {
        val fixed = fixHour(decimalHours + 0.5 / 60.0)
        val h = fixed.toInt().coerceIn(0, 23)
        val m = ((fixed - h) * 60.0).toInt().coerceIn(0, 59)
        val itemCal = (baseCal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, m)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return PrayerTimeItem(name, h, m, itemCal.timeInMillis)
    }

    private fun julianDate(y: Int, m: Int, d: Int): Double {
        var year = y
        var month = m
        if (month <= 2) {
            year -= 1
            month += 12
        }
        val a = floor(year / 100.0)
        val b = 2.0 - a + floor(a / 4.0)
        return floor(365.25 * (year + 4716)) + floor(30.6001 * (month + 1)) + d + b - 1524.5
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

    private fun hourAngle(angle: Double, lat: Double, decl: Double): Double {
        val num = -sinDeg(angle) - sinDeg(lat) * sinDeg(decl)
        val den = cosDeg(lat) * cosDeg(decl)
        val cosVal = (num / den).coerceIn(-1.0, 1.0)
        return acosDeg(cosVal) / 15.0
    }

    private fun asrTimeOffset(shadowFactor: Double, lat: Double, decl: Double): Double {
        val angle = -acotDeg(shadowFactor + tanDeg(abs(lat - decl)))
        val num = -sinDeg(angle) - sinDeg(lat) * sinDeg(decl)
        val den = cosDeg(lat) * cosDeg(decl)
        val cosVal = (num / den).coerceIn(-1.0, 1.0)
        return acosDeg(cosVal) / 15.0
    }

    private fun sinDeg(d: Double) = sin(Math.toRadians(d))
    private fun cosDeg(d: Double) = cos(Math.toRadians(d))
    private fun tanDeg(d: Double) = tan(Math.toRadians(d))
    private fun asinDeg(x: Double) = Math.toDegrees(asin(x))
    private fun acosDeg(x: Double) = Math.toDegrees(acos(x))
    private fun atan2Deg(y: Double, x: Double) = Math.toDegrees(atan2(y, x))
    private fun acotDeg(x: Double) = Math.toDegrees(atan(1.0 / x))
    private fun fixAngle(a: Double): Double = a - 360.0 * floor(a / 360.0)
    private fun fixHour(h: Double): Double = h - 24.0 * floor(h / 24.0)

    fun formatCountdown(millis: Long): String {
        val totalSec = (millis / 1000L).coerceAtLeast(0L)
        val h = totalSec / 3600L
        val m = (totalSec % 3600L) / 60L
        val s = totalSec % 60L
        return "%02d:%02d:%02d".format(h, m, s)
    }
}
