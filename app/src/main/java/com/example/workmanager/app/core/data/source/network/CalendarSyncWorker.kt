package com.example.workmanager.app.core.data.source.network

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.workmanager.app.core.data.source.local.CalendarEventDao
import com.example.workmanager.app.core.domain.model.AttendeeData
import com.example.workmanager.app.core.domain.model.AttendeeResponseStatus
import com.example.workmanager.app.core.domain.model.CalendarEvent
import com.example.workmanager.app.core.domain.model.EventStatus // Your app's local EventStatus
import com.example.workmanager.app.features.home.domain.repository.HomeRepository
import com.google.api.services.calendar.model.Event as GoogleApiEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class CalendarSyncWorker(
    private val appContext: Context, // Made private val as it's used in helper methods
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    private val homeRepository: HomeRepository by inject()
    private val calendarEventDao: CalendarEventDao by inject()

    companion object {
        const val WORK_NAME = "com.example.workmanager.app.worker.CalendarSyncWorker"
        private const val TAG = "CalendarSyncWorker"
        // private const val ACTION_EVENT_REMINDER_ALARM = "com.example.workmanager.ACTION_EVENT_REMINDER_ALARM" // For app-specific alarms
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "CalendarSyncWorker: Work execution started.")

        // homeRepository.getCalendarEventsFromGoogle() is now expected to return List<GoogleApiEvent>
        // This is a change from the original, where it returned Either<String, List<CalendarEvent>>
        // The repository method itself should just fetch, and the worker does the mapping and DB ops.
        // For now, let's assume getCalendarEventsFromGoogle() in the repository is adapted to return a raw list or Either<String, List<GoogleApiEvent>>

        val resultFromRepository =
            homeRepository.getCalendarEventsFromGoogle() // This now refers to the repo method that returns Either<String, List<CalendarEvent>> from previous context.
        // We will work with this assumption and it means the mapping to CalendarEvent happens in the repo.
        // The following logic will need to be adapted if the repo gives raw GoogleApiEvent.
        // For now, let's assume repo gives US List<CalendarEvent> that are ALREADY mapped, including provisional statuses.

        return resultFromRepository.fold(
            ifLeft = { errorMsg ->
                Log.e(TAG, "Calendar API sync failed: $errorMsg")
                if (errorMsg.contains("API Error: 401") || errorMsg.contains("API Error: 403") ||
                    errorMsg.contains("No last signed-in") || errorMsg.contains("GoogleSignInAccount is null")
                ) {
                    Log.w(
                        TAG,
                        "Authentication/Permission issue. Failing work. Next attempt via AlarmManager."
                    )
                } else {
                    Log.w(
                        TAG,
                        "Sync failed. Failing work. Next attempt via AlarmManager. Error: $errorMsg"
                    )
                }
                Result.failure()
            },
            ifRight = { eventsFromRepo -> // These are List<CalendarEvent> with provisional statuses set by repo's mapping
                Log.d(
                    TAG,
                    "Fetched ${eventsFromRepo.size} events from repository (already mapped). Processing for local app alarm status and DB."
                )
                try {
                    val finalEventsToUpsertInDb = mutableListOf<CalendarEvent>()

                    for (eventMappedByRepo in eventsFromRepo) {
                        val existingEventInDb =
                            calendarEventDao.getEventById(eventMappedByRepo.eventId)
                        var finalLocalAppStatus: EventStatus

                        val isGoogleEventCancelled =
                            eventMappedByRepo.googleCalendarApiStatus?.lowercase() == "cancelled"
                        val isEventUpcoming =
                            eventMappedByRepo.startTimeMillis > System.currentTimeMillis()

                        if (isGoogleEventCancelled) {
                            finalLocalAppStatus = EventStatus.CANCELLED
                            if (existingEventInDb?.eventStatus == EventStatus.SCHEDULED) {
                                Log.d(
                                    TAG,
                                    "Event ${eventMappedByRepo.eventId} API status is cancelled. Cancelling existing app alarm."
                                )
                                cancelAppLevelEventAlarm(appContext, eventMappedByRepo.eventId)
                            }
                        } else { // Google event is active (e.g., "confirmed", "tentative")
                            if (existingEventInDb == null) { // Brand new event to the app
                                finalLocalAppStatus =
                                    if (isEventUpcoming) EventStatus.PENDING else EventStatus.COMPLETED
                            } else { // Existing event in app's DB
                                if (didEventChangeForAlarming(
                                        existingEventInDb,
                                        eventMappedByRepo
                                    )
                                ) {
                                    if (existingEventInDb.eventStatus == EventStatus.SCHEDULED) {
                                        Log.d(
                                            TAG,
                                            "Event ${eventMappedByRepo.eventId} details changed. Cancelling old app alarm."
                                        )
                                        cancelAppLevelEventAlarm(
                                            appContext,
                                            eventMappedByRepo.eventId
                                        )
                                    }
                                    finalLocalAppStatus =
                                        if (isEventUpcoming) EventStatus.PENDING else EventStatus.COMPLETED
                                } else {
                                    // No critical changes for alarm. Preserve local status, with adjustments.
                                    finalLocalAppStatus = existingEventInDb.eventStatus
                                    if (finalLocalAppStatus == EventStatus.SCHEDULED && !isEventUpcoming) {
                                        finalLocalAppStatus =
                                            EventStatus.COMPLETED // Was scheduled, now past
                                    } else if (finalLocalAppStatus == EventStatus.PENDING && !isEventUpcoming) {
                                        finalLocalAppStatus =
                                            EventStatus.COMPLETED // Was pending, now past
                                    }
                                    // If user had set to COMPLETED or CANCELLED, it remains so unless major changes (handled by didEventChangeForAlarming)
                                }
                            }
                        }
                        finalEventsToUpsertInDb.add(eventMappedByRepo.copy(eventStatus = finalLocalAppStatus))
                    }

                    if (finalEventsToUpsertInDb.isNotEmpty()) {
                        calendarEventDao.upsertEvents(finalEventsToUpsertInDb)
                        Log.d(
                            TAG,
                            "Successfully upserted ${finalEventsToUpsertInDb.size} events to Room with final local app statuses."
                        )

                        for (processedEvent in finalEventsToUpsertInDb) {
                            if (processedEvent.eventStatus == EventStatus.PENDING && processedEvent.startTimeMillis > System.currentTimeMillis()) {
                                Log.d(
                                    TAG,
                                    "Event ${processedEvent.eventId} is PENDING. Attempting to schedule app alarm."
                                )
                                if (scheduleAppLevelEventAlarm(appContext, processedEvent)) {
                                    calendarEventDao.updateEventStatus(
                                        processedEvent.eventId,
                                        EventStatus.SCHEDULED
                                    )
                                } else {
                                    // Scheduling failed (e.g. too late, or other error)
                                    // Update to COMPLETED or a specific ERROR state if you add one
                                    calendarEventDao.updateEventStatus(
                                        processedEvent.eventId,
                                        EventStatus.COMPLETED
                                    )
                                }
                            }
                        }
                    } else {
                        Log.d(TAG, "No events to save after processing local app statuses.")
                    }
                    Result.success()
                } catch (e: Exception) {
                    Log.e(
                        TAG,
                        "Error during event processing for local app statuses or DB operations: ${e.message}",
                        e
                    )
                    Result.failure()
                }
            }
        )
    }

    private fun didEventChangeForAlarming(
        oldEvent: CalendarEvent,
        newEvent: CalendarEvent,
    ): Boolean {
        return oldEvent.startTimeMillis != newEvent.startTimeMillis ||
               oldEvent.eventName != newEvent.eventName || // If name changes, might re-notify
               oldEvent.googleCalendarApiStatus != newEvent.googleCalendarApiStatus // If API status (e.g. tentative->confirmed) changes
    }

    // Placeholder: Implement with actual AlarmManager logic
    private fun scheduleAppLevelEventAlarm(context: Context, event: CalendarEvent): Boolean {
        val reminderTimeMillis = event.startTimeMillis - (10 * 60 * 1000) // 15 mins before
        if (reminderTimeMillis <= System.currentTimeMillis()) {
            Log.w(
                TAG,
                "Cannot schedule alarm for event ${event.eventId}, reminder time is in the past."
            )
            return false
        }
        // Actual AlarmManager scheduling logic here...
        Log.d(
            TAG,
            "Placeholder: scheduleAppLevelEventAlarm for event ${event.eventId} at $reminderTimeMillis - SIMULATING SUCCESS"
        )
        return true
    }

    // Placeholder: Implement with actual AlarmManager logic
    private fun cancelAppLevelEventAlarm(context: Context, eventId: String) {
        // Actual AlarmManager cancellation logic here...
        Log.d(TAG, "Placeholder: cancelAppLevelEventAlarm for event $eventId")
    }
}