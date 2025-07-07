package com.bhavikm.calarm

import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.bhavikm.calarm.app.core.data.source.network.CalendarEventsSyncWorker
import com.bhavikm.calarm.app.core.service.AuthService
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
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
        if (action == "calendar_updates") {
            enqueueCalendarSync()
        }

        /*remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notificationBuilder =
                NotificationCompat.Builder(this, it.channelId!!)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(it.title)
                    .setContentText(it.body)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)

            notificationManager.notify(1001, notificationBuilder.build())
        }*/
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


    override fun onDestroy() {
        serviceJob.cancel() // Cancel all coroutines when the service is destroyed
        super.onDestroy()
    }
}