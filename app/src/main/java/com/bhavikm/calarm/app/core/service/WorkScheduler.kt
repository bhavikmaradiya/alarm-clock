package com.bhavikm.calarm.app.core.service

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.bhavikm.calarm.app.core.data.source.network.CalendarEventsSyncWorker
import java.util.concurrent.TimeUnit

class WorkScheduler(
    private val context: Context,
    private val authService: AuthService,
) {

    companion object {
        private const val TAG = "WorkScheduler"
    }

    fun scheduleWorker() {
        val workManager = WorkManager.Companion.getInstance(context)
        workManager.cancelUniqueWork(CalendarEventsSyncWorker.Companion.WORKER_NAME)
        if (authService.isUserSignedIn) {
            val hourlyWorkRequest =
                PeriodicWorkRequestBuilder<CalendarEventsSyncWorker>(
                    CalendarEventsSyncWorker.Companion.SYNC_INTERVAL_MINUTES.toLong(),
                    TimeUnit.MINUTES,
                )
                    .setConstraints(
                        Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED)
                            .build(),
                    )
                    .build()

            workManager.enqueueUniquePeriodicWork(
                CalendarEventsSyncWorker.Companion.WORKER_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                hourlyWorkRequest,
            )
            Log.d(TAG, "WorkManager scheduled")
        } else {
            Log.d(TAG, "WorkManager not scheduled because user is not logged in")
        }
    }
}