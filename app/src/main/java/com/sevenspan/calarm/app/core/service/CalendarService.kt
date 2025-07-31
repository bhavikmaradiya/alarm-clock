package com.sevenspan.calarm.app.core.service

import android.content.Context
import android.util.Log
import com.meticha.triggerx.TriggerXAlarmScheduler
import com.sevenspan.calarm.app.core.data.model.toRoomList
import com.sevenspan.calarm.app.core.data.repository.SettingsRepository
import com.sevenspan.calarm.app.core.data.source.local.CalendarEventDao
import com.sevenspan.calarm.app.core.model.CalendarEvent
import com.sevenspan.calarm.app.core.model.EventStatus
import com.sevenspan.calarm.app.core.service.SubscriptionService.Companion.ALARM_LIMIT_NON_PRO
import com.sevenspan.calarm.app.data.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

interface CalendarService {
    suspend fun getCalendarEvents(): Result<List<CalendarEvent>>
    fun getLocalCalendarEvents(): Flow<List<CalendarEvent>>
}

class GoogleCalendarService(
    private val context: Context,
    private val calendarEventDao: CalendarEventDao,
    private val settingsRepository: SettingsRepository,
    val alarmScheduler: TriggerXAlarmScheduler,
    private val authService: AuthService,
    private val subscriptionService: SubscriptionService,
) : CalendarService {

    companion object {
        val TAG = GoogleCalendarService::class.simpleName
    }

    override suspend fun getCalendarEvents(): Result<List<CalendarEvent>> =
        withContext(Dispatchers.IO) {
            if (!subscriptionService.isPremiumUser()) {
                return@withContext Result.failure(Exception("Please upgrade to pro"))
            }
            try {
                val settings =
                    settingsRepository.getSettings().first()
                        .copy(lastSyncedTime = Clock.System.now().toEpochMilliseconds())
                settingsRepository.saveSettings(settings)
                alarmScheduler.cancelAllAlarms(context)
                calendarEventDao.deleteAllEvents()
                Log.d(TAG, "Fetching Google Calendar events with access token.")

                val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
                val now = OffsetDateTime.now(ZoneOffset.UTC)
                val timeMax = now.plusHours((24 * settings.defaultDaysToSyncFromNow).toLong())
                val response = ApiClient.apiService
                    .getCalendarEvents(
                        timeMin = now.format(formatter),
                        timeMax = timeMax.format(formatter),
                        userId = authService.currentUser!!.uid,
                    )

                val calendarList = response.body()?.toRoomList() ?: listOf()

                if (calendarList.isNotEmpty()) {
                    calendarEventDao.upsertListByEventId(calendarList)
                }
                Log.d(
                    TAG,
                    "Fetched ${calendarList.size} events. Saving to Room DB.",
                )
//                calendarList.proceedCalendarEvents(defaultDelayBeforeTriggerMinutes = settings.defaultDelayBeforeTriggerMinutes)

                Result.success(calendarList)
            } catch (e: Exception) {
                println("$TAG Unexpected Error: ${e.message} $e")
                Result.failure(e)
            }
        }

    override fun getLocalCalendarEvents(): Flow<List<CalendarEvent>> =
        calendarEventDao.getAllEvents()

    private suspend fun List<CalendarEvent>.proceedCalendarEvents(defaultDelayBeforeTriggerMinutes: Int) {
        if (isNotEmpty()) {
            try {
                val userIsPro = subscriptionService.isPremiumUser()
                val allEventsToUpsert = mutableListOf<CalendarEvent>()
                val (todayStartMillis, todayEndMillis) = subscriptionService.getTodayMillisRange()

                val initiallyScheduledTodayEvents =
                    calendarEventDao.getEventsByStatusAndDateRange(
                        EventStatus.SCHEDULED,
                        todayStartMillis,
                        todayEndMillis
                    )
                var currentScheduledTodayCount = initiallyScheduledTodayEvents.size
                Log.d(
                    TAG,
                    "Initially found $currentScheduledTodayCount events scheduled for today in DB."
                )


                for (eventMappedByRepo in this) {
                    val existingEventInDb =
                        calendarEventDao.getEventById(eventMappedByRepo.eventId)
                    var finalStatus = EventStatus.COMPLETED

                    val isGoogleEventCancelled =
                        eventMappedByRepo.googleCalendarApiStatus?.lowercase() == "cancelled"
                    val isEventUpcoming =
                        eventMappedByRepo.startTimeMillis > System.currentTimeMillis()

                    if (isGoogleEventCancelled) {
                        finalStatus = EventStatus.CANCELLED
                        if (existingEventInDb?.eventStatus == EventStatus.SCHEDULED) {
                            Log.d(
                                TAG,
                                "Event ${eventMappedByRepo.eventId} API status is cancelled. Cancelling existing app alarm."
                            )
                            cancelAppLevelEventAlarm(context, eventMappedByRepo.id)
                            if (existingEventInDb.startTimeMillis >= todayStartMillis && existingEventInDb.startTimeMillis < todayEndMillis) {
                                currentScheduledTodayCount =
                                    (currentScheduledTodayCount - 1).coerceAtLeast(0)
                                Log.d(
                                    TAG,
                                    "Decremented today's scheduled count to $currentScheduledTodayCount due to cancellation of ${existingEventInDb.eventId}"
                                )
                            }
                        }
                    } else {
                        if (existingEventInDb == null) { // Brand new event
                            finalStatus =
                                if (isEventUpcoming) EventStatus.PENDING else EventStatus.COMPLETED
                        } else {
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
                                        context,
                                        eventMappedByRepo.id
                                    )
                                    if (existingEventInDb.startTimeMillis >= todayStartMillis && existingEventInDb.startTimeMillis < todayEndMillis) {
                                        currentScheduledTodayCount =
                                            (currentScheduledTodayCount - 1).coerceAtLeast(0)
                                        Log.d(
                                            TAG,
                                            "Decremented today's scheduled count to $currentScheduledTodayCount due to change/cancellation of ${existingEventInDb.eventId}"
                                        )
                                    }
                                }
                                finalStatus =
                                    if (isEventUpcoming) EventStatus.PENDING else EventStatus.COMPLETED
                            } else { // No critical changes
                                finalStatus = existingEventInDb.eventStatus
                                if ((finalStatus == EventStatus.SCHEDULED || finalStatus == EventStatus.PENDING) && !isEventUpcoming) {
                                    finalStatus = EventStatus.COMPLETED
                                    if (existingEventInDb.eventStatus == EventStatus.SCHEDULED &&
                                        existingEventInDb.startTimeMillis >= todayStartMillis &&
                                        existingEventInDb.startTimeMillis < todayEndMillis
                                    ) {
                                        currentScheduledTodayCount =
                                            (currentScheduledTodayCount - 1).coerceAtLeast(0)
                                        Log.d(
                                            TAG,
                                            "Decremented today's scheduled count to $currentScheduledTodayCount as past scheduled event ${existingEventInDb.eventId} marked COMPLETED"
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (finalStatus == EventStatus.PENDING && isEventUpcoming) {
                        val isEventForToday =
                            eventMappedByRepo.startTimeMillis >= todayStartMillis &&
                            eventMappedByRepo.startTimeMillis < todayEndMillis

                        if (!isEventForToday) {
                            Log.d(
                                TAG,
                                "Event ${eventMappedByRepo.eventId} is PENDING but not for today. Status remains PENDING for future run. No system alarm scheduled now."
                            )
                            // finalStatus remains PENDING
                        } else { // PENDING and FOR TODAY
                            Log.d(
                                TAG,
                                "Event ${eventMappedByRepo.eventId} is PENDING and for TODAY. Current today count: $currentScheduledTodayCount."
                            )
                            val wasThisEventAlreadyScheduledToday =
                                initiallyScheduledTodayEvents.any { it.eventId == eventMappedByRepo.eventId }

                            val shouldApplyLimit = !userIsPro
                            var limitReachedForNewEvent = false
                            if (shouldApplyLimit) {
                                if (currentScheduledTodayCount >= ALARM_LIMIT_NON_PRO && !wasThisEventAlreadyScheduledToday) {
                                    limitReachedForNewEvent = true
                                }
                            }

                            if (!limitReachedForNewEvent) { // Either user is Pro, or limit not reached, or it's an update to an existing scheduled event
                                if (userIsPro && (currentScheduledTodayCount >= ALARM_LIMIT_NON_PRO && !wasThisEventAlreadyScheduledToday)) {
                                    Log.d(
                                        TAG,
                                        "Pro user: Bypassing today's alarm limit for event ${eventMappedByRepo.eventId}."
                                    )
                                }
                                if (scheduleAppLevelEventAlarm(
                                        context = context,
                                        event = eventMappedByRepo.copy(
                                            eventStatus = finalStatus /* still PENDING here for schedule func */
                                        ),
                                        defaultDelayBeforeTriggerInMinutes = defaultDelayBeforeTriggerMinutes
                                    )
                                ) {
                                    finalStatus = EventStatus.SCHEDULED
                                    if (!wasThisEventAlreadyScheduledToday) {
                                        currentScheduledTodayCount++ // Increment for new or if pro user adds beyond limit
                                        Log.d(
                                            TAG,
                                            "Event ${eventMappedByRepo.eventId} successfully scheduled for today. New today count: $currentScheduledTodayCount"
                                        )
                                    } else {
                                        Log.d(
                                            TAG,
                                            "Event ${eventMappedByRepo.eventId} (already scheduled for today) successfully re-scheduled/confirmed."
                                        )
                                    }
                                } else {
                                    finalStatus = EventStatus.COMPLETED // Scheduling failed
                                    Log.w(
                                        TAG,
                                        "System alarm scheduling failed for today's event ${eventMappedByRepo.eventId}. Status set to COMPLETED."
                                    )
                                    if (wasThisEventAlreadyScheduledToday) { // If it failed to reschedule, it's no longer considered as taking a slot from the initial count
                                        currentScheduledTodayCount =
                                            (currentScheduledTodayCount - 1).coerceAtLeast(0)
                                    }
                                }
                            } else { // Limit reached for a new event AND user is not Pro
                                Log.i(
                                    TAG,
                                    "Non-Pro user: Today's alarm limit (${ALARM_LIMIT_NON_PRO}) reached. Event ${eventMappedByRepo.eventId} (new for today) will not be scheduled in this run. Status remains PENDING."
                                )
                            }
                        }
                    }
                    allEventsToUpsert.add(eventMappedByRepo.copy(eventStatus = finalStatus))
                }

                if (allEventsToUpsert.isNotEmpty()) {
                    calendarEventDao.upsertEvents(allEventsToUpsert)
                    Log.d(
                        TAG,
                        "Successfully upserted ${allEventsToUpsert.size} events to Room with their final statuses."
                    )
                } else {
                    Log.d(TAG, "No events to upsert after processing.")
                }


            } catch (e: Exception) {
                Log.e(TAG, "Error during event processing or DB operations: ${e.message}", e)

            }
        }
    }

    private fun didEventChangeForAlarming(
        oldEvent: CalendarEvent,
        newEvent: CalendarEvent,
    ): Boolean = oldEvent.startTimeMillis != newEvent.startTimeMillis ||
                 oldEvent.endTimeMillis != newEvent.endTimeMillis ||
                 oldEvent.eventName != newEvent.eventName ||
                 oldEvent.notes != newEvent.notes ||
                 oldEvent.location != newEvent.location ||
                 oldEvent.attendees != newEvent.attendees ||
                 oldEvent.googleCalendarApiStatus != newEvent.googleCalendarApiStatus

    private suspend fun scheduleAppLevelEventAlarm(
        context: Context,
        event: CalendarEvent,
        defaultDelayBeforeTriggerInMinutes: Int,
    ): Boolean {
        val reminderTimeMillis =
            event.startTimeMillis - (defaultDelayBeforeTriggerInMinutes * 60 * 1000)
        val triggerTime = if (reminderTimeMillis > System.currentTimeMillis()) {
            reminderTimeMillis
        } else if (event.startTimeMillis > System.currentTimeMillis()) {
            event.startTimeMillis
        } else {
            return false
        }
        return alarmScheduler.scheduleAlarm(
            context = context,
            triggerAtMillis = triggerTime,
            alarmId = event.id
        )
    }

    private suspend fun cancelAppLevelEventAlarm(context: Context, eventId: Int) =
        alarmScheduler.cancelAlarm(context, eventId)
}
