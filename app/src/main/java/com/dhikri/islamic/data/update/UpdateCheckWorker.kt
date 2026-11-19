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
import java.util.concurrent.TimeUnit

class UpdateCheckWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val info = AppUpdater.checkForUpdate() ?: return Result.success()
        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastNotified = prefs.getString(KEY_LAST_NOTIFIED, null)
        if (lastNotified != info.latestVersion) {
            showUpdateNotification(applicationContext, info.latestVersion)
            prefs.edit().putString(KEY_LAST_NOTIFIED, info.latestVersion).apply()
        }
        return Result.success()
    }

    companion object {
        const val EXTRA_AUTO_UPDATE = "com.dhikri.islamic.EXTRA_AUTO_UPDATE"
        private const val WORK_NAME = "dhikri_periodic_update_check"
        private const val CHANNEL_ID = "dhikri_app_updates"
        private const val NOTIFICATION_ID = 3101
        private const val PREFS_NAME = "dhikri_update_prefs"
        private const val KEY_LAST_NOTIFIED = "last_notified_version"

        fun schedulePeriodicCheck(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(12, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun showUpdateNotification(context: Context, version: String) {
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
                    "تحديثات تطبيق ذِكري",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "إشعارات توفر إصدارات وتحديثات جديدة لتطبيق ذِكري"
                }
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.createNotificationChannel(channel)
            }

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_AUTO_UPDATE, true)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                NOTIFICATION_ID,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("تحديث جديد لتطبيق ذِكري (v$version)")
                .setContentText("وصل تحديث جديد! اضغط هنا للدخول وتنزيل التحديث تلقائياً")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("وصل الإصدار الجديد v$version من تطبيق ذِكري. اضغط على الإشعار للدخول وبدء تنزيل التحديث وتثبيته تلقائياً دون حذف بياناتك.")
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            runCatching {
                NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            }
        }
    }
}
