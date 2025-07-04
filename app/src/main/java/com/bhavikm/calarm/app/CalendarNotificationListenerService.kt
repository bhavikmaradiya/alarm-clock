package com.bhavikm.calarm.app

import android.app.Notification
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.bhavikm.calarm.app.core.data.source.network.CalendarEventsSyncWorker

class CalendarNotificationListenerService : NotificationListenerService() {

    override fun onCreate() {
        super.onCreate()
        Log.d("NotificationListener", "✅ Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        Log.d("NotificationListener", "📢 Notification posted ${sbn?.packageName}")
        val notification = sbn?.notification ?: return
        val extras = notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE)
        val channelId = notification.channelId

        if (sbn.packageName == applicationContext.packageName && (title == "Calendar updated" || channelId == "calendar_updates")) {
            enqueueCalendarSync()
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("NotificationListener", "✅ Listener connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d("NotificationListener", "❌ Listener disconnected")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("NotificationListener", "❌ Service destroyed")
    }

    private fun enqueueCalendarSync() {
        val workRequest = OneTimeWorkRequestBuilder<CalendarEventsSyncWorker>()
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()

        WorkManager.getInstance(applicationContext)
            .enqueueUniqueWork(
                CalendarEventsSyncWorker.Companion.WORKER_NAME,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
    }
}
