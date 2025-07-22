package com.sevenspan.calarm

import android.app.NotificationManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.sevenspan.calarm.app.core.service.AuthService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val authService by inject<AuthService>()
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    companion object {
        private const val TAG = "MyFirebaseMsgService"
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        serviceScope.launch { authService.updateFcmToken(token) }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")

        val action = remoteMessage.data["action"]
        /*if (action == "calendar_updates") {
            enqueueCalendarSync()
        }*/

        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            val notificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val notificationBuilder =
                NotificationCompat.Builder(this, it.channelId!!)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(it.title)
                    .setContentText(it.body)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setAutoCancel(true)

            notificationManager.notify(1001, notificationBuilder.build())
        }
    }

    override fun onDestroy() {
        serviceJob.cancel()
        super.onDestroy()
    }
}
