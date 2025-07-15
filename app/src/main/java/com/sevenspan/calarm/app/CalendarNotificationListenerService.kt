package com.sevenspan.calarm.app

import android.app.Notification
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.sevenspan.calarm.app.core.service.WorkScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class CalendarNotificationListenerService : NotificationListenerService() {

    val workScheduler by inject<WorkScheduler>()
    val coroutineScope = CoroutineScope(Dispatchers.IO)

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

        if (sbn.packageName == applicationContext.packageName &&
            (title == "Calendar updated" || channelId == "calendar_updates")
        ) {
            coroutineScope.launch { workScheduler.enqueueCalendarSync() }
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
}
