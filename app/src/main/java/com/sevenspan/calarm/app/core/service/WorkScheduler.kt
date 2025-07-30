package com.sevenspan.calarm.app.core.service

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.sevenspan.calarm.app.core.data.source.network.CalendarEventsSyncWorker
import java.util.concurrent.TimeUnit

class WorkScheduler(
    private val context: Context,
    private val authService: AuthService,
) {
    private val workManager = WorkManager.getInstance(context)

    companion object {
        private const val TAG = "WorkScheduler"
    }

    fun isXiaomiDevice(): Boolean = Build.MANUFACTURER.equals(
        "Xiaomi",
        ignoreCase = true
    )

    fun cancelWorker() {
        workManager
            .cancelUniqueWork(CalendarEventsSyncWorker.Companion.RECURRING_WORKER_NAME)
    }

    suspend fun scheduleWorker() {
        if (!isXiaomiDevice()) {
            return
        }
        cancelWorker()
        if (authService.isUserSignedIn()) {
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
                CalendarEventsSyncWorker.Companion.RECURRING_WORKER_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                hourlyWorkRequest,
            )
            Log.d(TAG, "WorkManager scheduled")
        } else {
            Log.d(TAG, "WorkManager not scheduled because user is not logged in")
        }
    }

    suspend fun enqueueCalendarSync() {
        if (authService.isUserSignedIn()) {
            val workRequest = OneTimeWorkRequestBuilder<CalendarEventsSyncWorker>()
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .build()

            workManager
                .enqueueUniqueWork(
                    CalendarEventsSyncWorker.Companion.WORKER_NAME,
                    ExistingWorkPolicy.REPLACE,
                    workRequest,
                )
        }
    }
}
