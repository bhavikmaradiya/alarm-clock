package com.bhavikm.calarm.app.core.data.source.network

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bhavikm.calarm.CalarmApp.Companion.isNetworkAvailable
import com.bhavikm.calarm.app.core.data.repository.SettingsRepository
import com.bhavikm.calarm.app.core.service.AnalyticsService
import com.bhavikm.calarm.app.core.service.AuthService
import com.bhavikm.calarm.app.core.service.CalendarService
import com.meticha.triggerx.TriggerXAlarmScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.datetime.Clock
import org.koin.core.component.KoinComponent

class CalendarEventsSyncWorker(
    private val appContext: Context,
    private val workerParams: WorkerParameters,
    private val calendarService: CalendarService,
    private val settingsRepository: SettingsRepository,
    private val alarmScheduler: TriggerXAlarmScheduler,
    private val analytics: AnalyticsService,
    private val authService: AuthService,
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    companion object {
        const val WORKER_NAME = "com.example.workmanager.app.worker.CalendarSyncWorker"
        private const val TAG = "CalendarSyncWorker"

        const val SYNC_INTERVAL_MINUTES = 15
        private const val MINUTE_IN_MILLIS = SYNC_INTERVAL_MINUTES * 60 * 1000
    }

    override suspend fun doWork(): Result {
        if (!isNetworkAvailable(appContext)) {
            Log.e(TAG, "CalendarSyncWorker: No network available.")
            return Result.failure()
        }
        analytics.logEvent(TAG) {
            put("Work", "Work execution started")
            put("User", authService.currentUser?.email ?: "Unknown")
        }
        Log.d(TAG, "CalendarSyncWorker: Work execution started.")

        val appSettings = settingsRepository.getSettings().first()
        val currentTime = Clock.System.now().toEpochMilliseconds()

        if (appSettings.lastSyncedTime != null &&
            (currentTime - appSettings.lastSyncedTime <= MINUTE_IN_MILLIS)
        ) {
            Log.d(
                TAG,
                "Skipping sync, last sync was at ${appSettings.lastSyncedTime}, which is less than a minute ago from $currentTime.",
            )
            return Result.failure()
        }

        val resultFromRepository =
            calendarService.getCalendarEvents()

        return resultFromRepository.fold(
            onFailure = { e ->
                Log.e(TAG, "Calendar API sync failed: ${e.message}")
                Result.failure()
            },
            onSuccess = { events ->
                val calendarEvents =
                    calendarService.getLocalCalendarEvents().firstOrNull() ?: emptyList()
                if (calendarEvents.isNotEmpty()) {
                    val pairOfEvents = calendarEvents.mapNotNull { event ->
                        val reminderTimeMillis =
                            event.startTimeMillis -
                            (appSettings.defaultDelayBeforeTriggerMinutes * 60 * 1000)
                        if (reminderTimeMillis > System.currentTimeMillis()) {
                            Pair(event.id, reminderTimeMillis)
                        } else {
                            null
                        }
                    }
                    if (pairOfEvents.isNotEmpty()) {
                        alarmScheduler.scheduleAlarms(
                            context = appContext,
                            events = pairOfEvents,
                        )
                        analytics.logEvent(TAG) {
                            put("Work", "Event scheduled")
                            put("Alarm scheduled count", pairOfEvents.size.toDouble())
                        }
                        println("Event scheduled for ${pairOfEvents.size} events!!")
                    }
                } else {
                    Log.d(TAG, "CalendarSyncWorker: No events found in local database.")
                }
                Result.success()
            },
        )
    }
}
