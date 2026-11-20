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
import com.dhikri.islamic.domain.util.PrayerTimesCalculator

class PreAdhanReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = Result.success()

    companion object {
        const val ACTION_PRAYER_ALARM = "com.dhikri.islamic.ACTION_PRAYER_ALARM"
        const val EXTRA_PRAYER_NAME = "prayer_name"
        const val EXTRA_PRAYER_TIME = "prayer_time"
        const val EXTRA_CITY_NAME = "city_name"
        private const val LEGACY_WORK_NAME = "dhikri_pre_adhan_reminder"
        private const val CHANNEL_ID = "dhikri_prayer_reminders_high"
        private const val NOTIFICATION_ID = 3401
        private const val ALARM_REQ_CODE = 3402
        private const val PREFS_NAME = "dhikri_prayer_alarm_prefs"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_CITY = "city_name"

        fun scheduleNext(context: Context, cityName: String, fromMillis: Long = System.currentTimeMillis()) {
            // Cancel any legacy WorkManager job
            runCatching { WorkManager.getInstance(context).cancelUniqueWork(LEGACY_WORK_NAME) }

            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_ENABLED, true)
                .putString(KEY_CITY, cityName)
                .apply()

            val schedule = PrayerTimesCalculator.computeSchedule(cityName, fromMillis)
            // Fire 10 minutes before the next prayer (or at the prayer time if less than 10 min left)
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
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                ALARM_REQ_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val showIntent = PendingIntent.getActivity(
                context,
                ALARM_REQ_CODE + 1,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            runCatching {
                // setAlarmClock bypasses Doze & Realme/OEM battery throttling reliably
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

            val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                action = ACTION_PRAYER_ALARM
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                ALARM_REQ_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
        }

        fun rescheduleIfEnabled(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            if (prefs.getBoolean(KEY_ENABLED, false)) {
                val city = prefs.getString(KEY_CITY, "عمّان") ?: "عمّان"
                scheduleNext(context, city)
            }
        }

        fun showPreAdhanNotification(context: Context, prayerName: String, timeStr: String) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted) return
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "تنبيهات مواقيت الصلاة",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "تنبيه دقيق قبل موعد الأذان بـ 10 دقائق حتى والهاتف مقفول"
                }
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.createNotificationChannel(channel)
            }

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pi = PendingIntent.getActivity(
                context,
                NOTIFICATION_ID,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("اقترب موعد صلاة $prayerName")
                .setContentText("متبقي 10 دقائق على أذان $prayerName ($timeStr) — استعد للصلاة")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .build()

            runCatching {
                NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            }
        }
    }
}
