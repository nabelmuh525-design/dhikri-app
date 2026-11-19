package com.dhikri.islamic.data.update

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.dhikri.islamic.MainActivity
import com.dhikri.islamic.R
import java.util.concurrent.TimeUnit

class UpdateCheckWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val info = AppUpdater.checkForUpdate() ?: return Result.success()
        notifyIfNew(applicationContext, info.latestVersion)
        return Result.success()
    }

    companion object {
        const val EXTRA_AUTO_UPDATE = "com.dhikri.islamic.EXTRA_AUTO_UPDATE"
        private const val PERIODIC_WORK_NAME = "dhikri_periodic_update_check"
        private const val IMMEDIATE_WORK_NAME = "dhikri_immediate_update_check"
        private const val CHANNEL_ID = "dhikri_app_updates"
        private const val NOTIFICATION_ID = 3101
        private const val PREFS_NAME = "dhikri_update_prefs"
        private const val KEY_LAST_NOTIFIED = "last_notified_version"

        fun schedulePeriodicCheck(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val periodic = PeriodicWorkRequestBuilder<UpdateCheckWorker>(12, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodic
            )

            // Also check shortly after launch whenever internet is connected
            val immediate = OneTimeWorkRequestBuilder<UpdateCheckWorker>()
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                immediate
            )
        }

        fun notifyIfNew(context: Context, version: String) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val lastNotified = prefs.getString(KEY_LAST_NOTIFIED, null)
            if (lastNotified != version) {
                if (showUpdateNotification(context, version)) {
                    prefs.edit().putString(KEY_LAST_NOTIFIED, version).apply()
                }
            }
        }

        fun showUpdateNotification(context: Context, version: String): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted) return false
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

            val largeIcon = runCatching {
                BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)
            }.getOrNull()

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .apply { if (largeIcon != null) setLargeIcon(largeIcon) }
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

            return runCatching {
                NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
                true
            }.getOrDefault(false)
        }
    }
}
