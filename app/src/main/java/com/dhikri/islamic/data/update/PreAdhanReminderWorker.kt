package com.dhikri.islamic.data.update

import android.Manifest
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
import androidx.work.*
import com.dhikri.islamic.MainActivity
import com.dhikri.islamic.domain.util.PrayerTimesCalculator
import java.util.concurrent.TimeUnit

class PreAdhanReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val prayerName = inputData.getString(KEY_PRAYER_NAME) ?: "الصلاة"
        val prayerTime = inputData.getString(KEY_PRAYER_TIME) ?: ""
        val cityName = inputData.getString(KEY_CITY_NAME) ?: "عمّان"

        showPreAdhanNotification(applicationContext, prayerName, prayerTime)
        // Schedule the following prayer reminder automatically
        scheduleNext(applicationContext, cityName, System.currentTimeMillis() + 12 * 60_000L)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "dhikri_pre_adhan_reminder"
        private const val CHANNEL_ID = "dhikri_prayer_reminders"
        private const val NOTIFICATION_ID = 3401
        private const val KEY_PRAYER_NAME = "prayer_name"
        private const val KEY_PRAYER_TIME = "prayer_time"
        private const val KEY_CITY_NAME = "city_name"

        fun scheduleNext(context: Context, cityName: String, fromMillis: Long = System.currentTimeMillis()) {
            val schedule = PrayerTimesCalculator.computeSchedule(cityName, fromMillis)
            val targetTrigger = schedule.nextPrayer.timeMillis - 10 * 60_000L
            val delayMs = (targetTrigger - System.currentTimeMillis()).coerceAtLeast(15_000L)

            val data = workDataOf(
                KEY_PRAYER_NAME to schedule.nextPrayer.nameArabic,
                KEY_PRAYER_TIME to schedule.nextPrayer.formatted12h(),
                KEY_CITY_NAME to cityName
            )
            val req = OneTimeWorkRequestBuilder<PreAdhanReminderWorker>()
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                req
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        private fun showPreAdhanNotification(context: Context, prayerName: String, timeStr: String) {
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
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "تنبيه لطيف قبل موعد الأذان بـ 10 دقائق"
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
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .build()

            runCatching {
                NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            }
        }
    }
}
