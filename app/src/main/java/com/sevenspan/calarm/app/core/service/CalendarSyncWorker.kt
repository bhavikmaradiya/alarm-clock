package com.sevenspan.calarm.app.core.service // Assuming this is the correct package

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.koin.core.component.KoinComponent

class CalendarSyncWorker(
    private val appContext: Context,
    workerParams: WorkerParameters,
    val calendarService: CalendarService,
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    companion object {
        const val WORK_NAME = "com.example.workmanager.app.worker.CalendarSyncWorker"
        private const val TAG = "CalendarSyncWorker"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "CalendarSyncWorker: Work execution started.")
        val resultFromRepository = calendarService.getCalendarEvents()

        return resultFromRepository.fold(
            onFailure = { errorMsg ->
                Log.e(TAG, "Calendar API sync failed: $errorMsg")
                Result.failure()
            },
            onSuccess = { eventsFromRepo ->
                Log.d(TAG, "Fetched ${eventsFromRepo.size} events from repository. Processing...")
                Result.success()
            }
        )
    }
}
