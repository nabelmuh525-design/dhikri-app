package com.dhikri.islamic.data.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PrayerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        when (action) {
            PreAdhanReminderWorker.ACTION_PRAYER_ALARM -> {
                val prayerName = intent.getStringExtra(PreAdhanReminderWorker.EXTRA_PRAYER_NAME) ?: "الصلاة"
                val prayerTime = intent.getStringExtra(PreAdhanReminderWorker.EXTRA_PRAYER_TIME) ?: ""
                val cityName = intent.getStringExtra(PreAdhanReminderWorker.EXTRA_CITY_NAME) ?: "عمّان"
                val scheduledAt = intent.getLongExtra(PreAdhanReminderWorker.EXTRA_SCHEDULED_AT, System.currentTimeMillis())
                PreAdhanReminderWorker.showPreAdhanNotification(context, prayerName, prayerTime, scheduledAt)
                PreAdhanReminderWorker.schedulePreAdhanOnly(context, cityName, System.currentTimeMillis() + 12 * 60_000L)
            }
            PreAdhanReminderWorker.ACTION_POST_PRAYER_DHIKR -> {
                val prayerName = intent.getStringExtra(PreAdhanReminderWorker.EXTRA_PRAYER_NAME) ?: "الصلاة"
                val cityName = intent.getStringExtra(PreAdhanReminderWorker.EXTRA_CITY_NAME) ?: "عمّان"
                val scheduledAt = intent.getLongExtra(PreAdhanReminderWorker.EXTRA_SCHEDULED_AT, System.currentTimeMillis())
                PreAdhanReminderWorker.showPostPrayerNotifications(context, prayerName, scheduledAt)
                PreAdhanReminderWorker.scheduleNextPostPrayerDhikr(context, cityName, System.currentTimeMillis() + 5 * 60_000L)
            }
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                PreAdhanReminderWorker.rescheduleIfEnabled(context)
            }
        }
    }
}
