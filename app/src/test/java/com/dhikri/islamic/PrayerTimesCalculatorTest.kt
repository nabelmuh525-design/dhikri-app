package com.dhikri.islamic

import com.dhikri.islamic.domain.util.*
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class PrayerTimesCalculatorTest {

    private fun dateInCity(city: PrayerCity, year: Int, month1Based: Int, day: Int): Calendar {
        return Calendar.getInstance(TimeZone.getTimeZone(city.timeZoneId)).apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month1Based - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    @Test
    fun amman_jordanAwqaf_monotonicAndRealistic() {
        val city = PrayerTimesCalculator.cityByName("عمّان")
        val prayers = PrayerTimesCalculator.computeForDate(
            city,
            dateInCity(city, 2026, 3, 21),
            PrayerCalculationConfig(method = CalculationMethod.JORDAN_AWQAF)
        )
        assertEquals(5, prayers.size)
        for (i in 0 until 4) {
            assertTrue("Prayer ${prayers[i].nameArabic} must precede ${prayers[i + 1].nameArabic}",
                prayers[i].timeMillis < prayers[i + 1].timeMillis)
        }
        assertTrue("Amman Fajr in March should be between 4:00 and 6:00", prayers[0].hour24 in 4..5)
        assertTrue("Amman Dhuhr in March should be between 12:00 and 13:59", prayers[1].hour24 in 12..13)
    }

    @Test
    fun makkah_madinah_cairo_london_newYork_allValidAndMonotonic() {
        val testCities = listOf("مكة المكرمة", "المدينة المنورة", "القاهرة", "لندن", "نيويورك")
        testCities.forEach { name ->
            val city = PrayerTimesCalculator.cityByName(name)
            val prayers = PrayerTimesCalculator.computeForDate(
                city,
                dateInCity(city, 2026, 6, 21),
                PrayerCalculationConfig(method = city.defaultMethod)
            )
            assertEquals(5, prayers.size)
            for (i in 0 until 4) {
                assertTrue("$name ordering failed", prayers[i].timeMillis < prayers[i + 1].timeMillis)
            }
        }
    }

    @Test
    fun hanafiAsr_isLaterThanShafiAsr() {
        val city = PrayerTimesCalculator.cityByName("عمّان")
        val date = dateInCity(city, 2026, 6, 15)
        val shafi = PrayerTimesCalculator.computeForDate(city, date, PrayerCalculationConfig(asrMadhab = AsrMadhab.SHAFI))
        val hanafi = PrayerTimesCalculator.computeForDate(city, date, PrayerCalculationConfig(asrMadhab = AsrMadhab.HANAFI))
        assertTrue("Hanafi Asr must be later than Shafi Asr", hanafi[2].timeMillis > shafi[2].timeMillis)
    }

    @Test
    fun london_dstSummerVsWinter_shiftsDhuhrByApproxOneHour() {
        val london = PrayerTimesCalculator.cityByName("لندن")
        val winter = PrayerTimesCalculator.computeForDate(london, dateInCity(london, 2026, 1, 15))
        val summer = PrayerTimesCalculator.computeForDate(london, dateInCity(london, 2026, 7, 15))
        val winterDhuhrMin = winter[1].hour24 * 60 + winter[1].minute
        val summerDhuhrMin = summer[1].hour24 * 60 + summer[1].minute
        val diff = summerDhuhrMin - winterDhuhrMin
        assertTrue("London summer DST Dhuhr should be ~50-70 min later than winter (actual=$diff)", diff in 45..75)
    }

    @Test
    fun manualOffsets_shiftExactMinutes() {
        val city = PrayerTimesCalculator.cityByName("عمّان")
        val date = dateInCity(city, 2026, 4, 10)
        val base = PrayerTimesCalculator.computeForDate(city, date, PrayerCalculationConfig())
        val shifted = PrayerTimesCalculator.computeForDate(
            city,
            date,
            PrayerCalculationConfig(offsets = PrayerOffsets(fajr = 5, maghrib = -3))
        )
        assertEquals(5 * 60_000L, shifted[0].timeMillis - base[0].timeMillis)
        assertEquals(-3 * 60_000L, shifted[3].timeMillis - base[3].timeMillis)
    }

    @Test
    fun highLatitude_polarSummer_producesMonotonicNonNaNResults() {
        val oslo = PrayerCity("أوسلو", 59.9139, 10.7522, "Europe/Oslo", CalculationMethod.MWL)
        val tromso = PrayerCity("ترومسو", 69.6492, 18.9553, "Europe/Oslo", CalculationMethod.MWL)
        listOf(oslo, tromso).forEach { city ->
            val prayers = PrayerTimesCalculator.computeForDate(
                city,
                dateInCity(city, 2026, 6, 21),
                PrayerCalculationConfig(method = CalculationMethod.MWL, highLatitudeRule = HighLatitudeRule.ANGLE_BASED)
            )
            assertEquals(5, prayers.size)
            for (i in 0 until 4) {
                assertTrue("Polar monotonic ordering failed for ${city.nameArabic}", prayers[i].timeMillis < prayers[i + 1].timeMillis)
            }
        }
    }

    @Test
    fun midnightCrossing_selectsTomorrowFajr() {
        val city = PrayerTimesCalculator.cityByName("عمّان")
        val lateNight = Calendar.getInstance(TimeZone.getTimeZone(city.timeZoneId)).apply {
            set(2026, Calendar.MAY, 10, 23, 55, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val schedule = PrayerTimesCalculator.computeScheduleForCity(city, lateNight.timeInMillis)
        assertEquals("الفجر", schedule.nextPrayer.nameArabic)
        assertTrue("Next Fajr must be strictly in the future", schedule.nextPrayer.timeMillis > lateNight.timeInMillis)
    }
}
