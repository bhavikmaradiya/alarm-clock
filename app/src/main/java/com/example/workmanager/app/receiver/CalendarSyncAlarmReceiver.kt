package com.example.workmanager.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.workmanager.app.core.data.source.network.CalendarSyncWorker

class CalendarSyncAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val TAG = "CalendarSyncAlarmRcvr"
        // Define a unique action string for the intent that triggers this receiver
        const val ACTION_SYNC_CALENDAR_ALARM = "com.example.workmanager.ACTION_SYNC_CALENDAR_ALARM"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_SYNC_CALENDAR_ALARM) {
            Log.d(TAG, "Calendar sync alarm received. Enqueuing OneTimeWorkRequest for CalendarSyncWorker.")

            // Define constraints for the worker (e.g., network connectivity)
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val calendarSyncWorkRequest = OneTimeWorkRequestBuilder<CalendarSyncWorker>()
                .setConstraints(constraints)
                // You could add input data to the worker if needed using .setInputData(...)
                .build()

            WorkManager.getInstance(context).enqueue(calendarSyncWorkRequest)
            Log.d(TAG, "CalendarSyncWorker enqueued.")

            // IMPORTANT: For precise repeating alarms that survive Doze,
            // it's often more reliable to schedule the *next* alarm from here
            // after the current one has fired and the work is enqueued.
            // This involves calling the scheduling logic again.
            // For simplicity in this step, we'll use AlarmManager.setRepeating initially in App.kt,
            // but this is the place to chain setExactAndAllowWhileIdle calls.
        }
    }
}
