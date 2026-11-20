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
                PreAdhanReminderWorker.showPreAdhanNotification(context, prayerName, prayerTime)
                // Schedule the following prayer automatically (+12 min offset to move past current prayer)
                PreAdhanReminderWorker.scheduleNext(context, cityName, System.currentTimeMillis() + 12 * 60_000L)
            }
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                PreAdhanReminderWorker.rescheduleIfEnabled(context)
            }
        }
    }
}
