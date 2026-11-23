package com.dhikri.islamic.data.update

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.dhikri.islamic.MainActivity
import com.dhikri.islamic.data.preferences.UserSettings
import com.dhikri.islamic.domain.util.AsrMadhab
import com.dhikri.islamic.domain.util.CalculationMethod
import com.dhikri.islamic.domain.util.HighLatitudeRule
import com.dhikri.islamic.domain.util.PrayerCalculationConfig
import com.dhikri.islamic.domain.util.PrayerOffsets
import com.dhikri.islamic.domain.util.PrayerTimesCalculator

class PreAdhanReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = Result.success()

    companion object {
        const val ACTION_PRAYER_ALARM = "com.dhikri.islamic.ACTION_PRAYER_ALARM"
        const val ACTION_POST_PRAYER_DHIKR = "com.dhikri.islamic.ACTION_POST_PRAYER_DHIKR"
        const val EXTRA_PRAYER_NAME = "prayer_name"
        const val EXTRA_PRAYER_TIME = "prayer_time"
        const val EXTRA_CITY_NAME = "city_name"
        const val EXTRA_SCHEDULED_AT = "scheduled_at_ms"

        private const val LEGACY_WORK_NAME = "dhikri_pre_adhan_reminder"
        private const val CHANNEL_PRAYER_ID = "dhikri_prayer_reminders_high"
        private const val CHANNEL_ADHKAR_ID = "dhikri_adhkar_reminders_high"

        private const val NOTIF_ID_PRE_ADHAN = 3401
        private const val NOTIF_ID_POST_ISTIGHFAR = 3411
        private const val NOTIF_ID_ADHKAR = 3412

        private const val REQ_CODE_PRE_ADHAN = 3402
        private const val REQ_CODE_POST_DHIKR = 3422
        private const val MAX_STALE_ALARM_MS = 20 * 60_000L

        private const val PREFS_NAME = "dhikri_prayer_alarm_prefs"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_CITY = "city_name"
        private const val KEY_METHOD = "method_key"
        private const val KEY_MADHAB = "madhab_key"
        private const val KEY_HIGH_LAT = "high_lat_key"
        private const val KEY_LAST_PRE_KEY = "last_pre_sent_key"
        private const val KEY_LAST_POST_KEY = "last_post_sent_key"

        fun ensureChannels(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val prayerChannel = NotificationChannel(
                CHANNEL_PRAYER_ID,
                "تنبيهات الصلاة والاستغفار",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تنبيهات مواقيت الصلاة والاستغفار بعد كل صلاة"
                enableVibration(true)
            }
            val adhkarChannel = NotificationChannel(
                CHANNEL_ADHKAR_ID,
                "تذكير أذكار الصباح والمساء",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تذكير بقراءة أذكار الصباح بعد الفجر وأذكار المساء بعد العشاء"
                enableVibration(true)
            }
            manager.createNotificationChannel(prayerChannel)
            manager.createNotificationChannel(adhkarChannel)
        }

        fun syncWithSettings(context: Context, settings: UserSettings) {
            ensureChannels(context)
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putBoolean(KEY_ENABLED, settings.preAdhanReminderEnabled)
                .putString(KEY_CITY, settings.prayerCityName)
                .putString(KEY_METHOD, settings.calculationMethodKey)
                .putString(KEY_MADHAB, settings.asrMadhabKey)
                .putString(KEY_HIGH_LAT, settings.highLatitudeRuleKey)
                .putInt("off_fajr", settings.offsetFajrMin)
                .putInt("off_dhuhr", settings.offsetDhuhrMin)
                .putInt("off_asr", settings.offsetAsrMin)
                .putInt("off_maghrib", settings.offsetMaghribMin)
                .putInt("off_isha", settings.offsetIshaMin)
                .putBoolean("n_الفجر", settings.notifyFajr)
                .putBoolean("n_الظهر", settings.notifyDhuhr)
                .putBoolean("n_العصر", settings.notifyAsr)
                .putBoolean("n_المغرب", settings.notifyMaghrib)
                .putBoolean("n_العشاء", settings.notifyIsha)
                .putBoolean("post_istighfar", settings.postPrayerIstighfarEnabled)
                .putBoolean("post_adhkar", settings.morningEveningAdhkarReminderEnabled)
                .apply()

            if (settings.preAdhanReminderEnabled) {
                scheduleNext(context, settings.prayerCityName)
            } else {
                cancel(context)
            }
        }

        fun ensureScheduledOnStartup(context: Context) {
            ensureChannels(context)
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val enabled = prefs.getBoolean(KEY_ENABLED, true)
            val city = prefs.getString(KEY_CITY, "عمّان") ?: "عمّان"
            if (enabled) {
                scheduleNext(context, city)
            } else {
                cancel(context)
            }
        }

        private fun readConfig(context: Context): PrayerCalculationConfig {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return PrayerCalculationConfig(
                method = CalculationMethod.fromKey(prefs.getString(KEY_METHOD, "jordan_awqaf")),
                asrMadhab = AsrMadhab.fromKey(prefs.getString(KEY_MADHAB, "shafi")),
                highLatitudeRule = HighLatitudeRule.fromKey(prefs.getString(KEY_HIGH_LAT, "angle_based")),
                offsets = PrayerOffsets(
                    fajr = prefs.getInt("off_fajr", 0),
                    dhuhr = prefs.getInt("off_dhuhr", 0),
                    asr = prefs.getInt("off_asr", 0),
                    maghrib = prefs.getInt("off_maghrib", 0),
                    isha = prefs.getInt("off_isha", 0)
                )
            )
        }

        fun isPrayerEnabled(context: Context, prayerName: String): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean("n_$prayerName", true)
        }

        fun scheduleNext(context: Context, cityName: String, fromMillis: Long = System.currentTimeMillis()) {
            ensureChannels(context)
            runCatching { WorkManager.getInstance(context).cancelUniqueWork(LEGACY_WORK_NAME) }

            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_ENABLED, true)
                .putString(KEY_CITY, cityName)
                .apply()

            schedulePreAdhanOnly(context, cityName, fromMillis)
            scheduleNextPostPrayerDhikr(context, cityName, fromMillis)
        }

        fun schedulePreAdhanOnly(context: Context, cityName: String, fromMillis: Long = System.currentTimeMillis()) {
            val config = readConfig(context)
            val schedule = PrayerTimesCalculator.computeSchedule(cityName, fromMillis, config)
            val preTrigger = schedule.nextPrayer.timeMillis - 10 * 60_000L
            val triggerAt = if (preTrigger > System.currentTimeMillis() + 5_000L) {
                preTrigger
            } else {
                schedule.nextPrayer.timeMillis.coerceAtLeast(System.currentTimeMillis() + 10_000L)
            }

            val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                action = ACTION_PRAYER_ALARM
                putExtra(EXTRA_PRAYER_NAME, schedule.nextPrayer.nameArabic)
                putExtra(EXTRA_PRAYER_TIME, schedule.nextPrayer.formatted12h())
                putExtra(EXTRA_CITY_NAME, cityName)
                putExtra(EXTRA_SCHEDULED_AT, triggerAt)
            }
            scheduleExactAlarm(context, REQ_CODE_PRE_ADHAN, triggerAt, intent)
        }

        fun scheduleNextPostPrayerDhikr(context: Context, cityName: String, fromMillis: Long = System.currentTimeMillis()) {
            val config = readConfig(context)
            val offsetAfterPrayerMs = 15 * 60_000L
            val probeTime = fromMillis - offsetAfterPrayerMs + 5_000L
            val schedule = PrayerTimesCalculator.computeSchedule(cityName, probeTime, config)
            val targetPrayer = schedule.nextPrayer
            val triggerAt = (targetPrayer.timeMillis + offsetAfterPrayerMs)
                .coerceAtLeast(System.currentTimeMillis() + 15_000L)

            val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                action = ACTION_POST_PRAYER_DHIKR
                putExtra(EXTRA_PRAYER_NAME, targetPrayer.nameArabic)
                putExtra(EXTRA_PRAYER_TIME, targetPrayer.formatted12h())
                putExtra(EXTRA_CITY_NAME, cityName)
                putExtra(EXTRA_SCHEDULED_AT, triggerAt)
            }
            scheduleExactAlarm(context, REQ_CODE_POST_DHIKR, triggerAt, intent)
        }

        private fun scheduleExactAlarm(context: Context, requestCode: Int, triggerAt: Long, intent: Intent) {
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val showIntent = PendingIntent.getActivity(
                context,
                requestCode + 1,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            runCatching {
                val info = AlarmManager.AlarmClockInfo(triggerAt, showIntent)
                alarmManager.setAlarmClock(info, pendingIntent)
            }.onFailure {
                runCatching {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                    } else {
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                    }
                }.onFailure {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                    } else {
                        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                    }
                }
            }
        }

        fun cancel(context: Context) {
            runCatching { WorkManager.getInstance(context).cancelUniqueWork(LEGACY_WORK_NAME) }
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_ENABLED, false)
                .apply()

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            listOf(
                REQ_CODE_PRE_ADHAN to ACTION_PRAYER_ALARM,
                REQ_CODE_POST_DHIKR to ACTION_POST_PRAYER_DHIKR
            ).forEach { (reqCode, act) ->
                val intent = Intent(context, PrayerAlarmReceiver::class.java).apply { action = act }
                val pi = PendingIntent.getBroadcast(
                    context,
                    reqCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.cancel(pi)
            }
        }

        fun rescheduleIfEnabled(context: Context) {
            ensureScheduledOnStartup(context)
        }

        private fun hasNotificationPermission(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        }

        fun showPreAdhanNotification(
            context: Context,
            prayerName: String,
            timeStr: String,
            scheduledAtMs: Long = System.currentTimeMillis()
        ) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            if (!prefs.getBoolean(KEY_ENABLED, true)) return
            if (!isPrayerEnabled(context, prayerName)) return
            // Never fire stale alarm after prayer window passed
            if (System.currentTimeMillis() - scheduledAtMs > MAX_STALE_ALARM_MS) return
            // Deduplicate per prayer slot
            val dedupKey = "${prayerName}_${scheduledAtMs / 60_000L}"
            if (prefs.getString(KEY_LAST_PRE_KEY, null) == dedupKey) return
            if (!hasNotificationPermission(context)) return
            ensureChannels(context)

            val pi = mainPendingIntent(context, NOTIF_ID_PRE_ADHAN)
            val notification = NotificationCompat.Builder(context, CHANNEL_PRAYER_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("اقترب موعد صلاة $prayerName")
                .setContentText("تبقّى دقائق على أذان $prayerName ($timeStr) — استعد للصلاة")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .build()

            runCatching {
                NotificationManagerCompat.from(context).notify(NOTIF_ID_PRE_ADHAN, notification)
                prefs.edit().putString(KEY_LAST_PRE_KEY, dedupKey).apply()
            }
        }

        fun showPostPrayerNotifications(
            context: Context,
            prayerName: String,
            scheduledAtMs: Long = System.currentTimeMillis()
        ) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            if (!prefs.getBoolean(KEY_ENABLED, true)) return
            if (System.currentTimeMillis() - scheduledAtMs > MAX_STALE_ALARM_MS) return
            val dedupKey = "${prayerName}_${scheduledAtMs / 60_000L}"
            if (prefs.getString(KEY_LAST_POST_KEY, null) == dedupKey) return
            if (!hasNotificationPermission(context)) return
            ensureChannels(context)

            if (prefs.getBoolean("post_istighfar", true)) {
                val istighfarPi = mainPendingIntent(context, NOTIF_ID_POST_ISTIGHFAR)
                val istighfarNotif = NotificationCompat.Builder(context, CHANNEL_PRAYER_ID)
                    .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                    .setContentTitle("وقت الاستغفار بعد صلاة $prayerName")
                    .setContentText("أستغفر الله العظيم وأتوب إليه — خصّص دقائق للاستغفار والأذكار بعد الصلاة")
                    .setStyle(
                        NotificationCompat.BigTextStyle().bigText(
                            "أستغفر الله العظيم الذي لا إله إلا هو الحي القيوم وأتوب إليه. افتح عداد الاستغفار في ذِكري الآن."
                        )
                    )
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setAutoCancel(true)
                    .setContentIntent(istighfarPi)
                    .build()

                runCatching {
                    NotificationManagerCompat.from(context).notify(NOTIF_ID_POST_ISTIGHFAR, istighfarNotif)
                }
            }

            if (prefs.getBoolean("post_adhkar", true)) {
                val adhkarReminder: Pair<String, String>? = when (prayerName) {
                    "الفجر" -> "لا تنسَ أذكار الصباح" to "بعد صلاة الفجر: اقرأ أذكار الصباح الآن لتحفظ يومك وتنال الطمأنينة"
                    "العشاء" -> "لا تنسَ أذكار المساء" to "بعد صلاة العشاء: اقرأ أذكار المساء الآن لتحصّن ليلتك بذكر الله"
                    else -> null
                }

                if (adhkarReminder != null) {
                    val adhkarPi = mainPendingIntent(context, NOTIF_ID_ADHKAR)
                    val adhkarNotif = NotificationCompat.Builder(context, CHANNEL_ADHKAR_ID)
                        .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                        .setContentTitle(adhkarReminder.first)
                        .setContentText(adhkarReminder.second)
                        .setStyle(NotificationCompat.BigTextStyle().bigText(adhkarReminder.second))
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setDefaults(NotificationCompat.DEFAULT_ALL)
                        .setAutoCancel(true)
                        .setContentIntent(adhkarPi)
                        .build()

                    runCatching {
                        NotificationManagerCompat.from(context).notify(NOTIF_ID_ADHKAR, adhkarNotif)
                    }
                }
            }
            prefs.edit().putString(KEY_LAST_POST_KEY, dedupKey).apply()
        }

        private fun mainPendingIntent(context: Context, requestCode: Int): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            return PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
