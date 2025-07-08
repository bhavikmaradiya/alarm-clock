package com.bhavikm.calarm.app.core.service

import android.accounts.Account
import android.content.Context
import android.util.Log
import com.bhavikm.calarm.app.core.data.repository.SettingsRepository
import com.bhavikm.calarm.app.core.data.source.local.CalendarEventDao
import com.bhavikm.calarm.app.core.model.AttendeeData
import com.bhavikm.calarm.app.core.model.AttendeeResponseStatus
import com.bhavikm.calarm.app.core.model.CalendarEvent
import com.bhavikm.calarm.app.core.model.EventStatus
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.Calendar
import com.meticha.triggerx.TriggerXAlarmScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import java.io.IOException

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
) : CalendarService {

    private var _calendarService: Calendar? = null
    private var lastUsedAccount: Account? = null

    /* val calendarService: Calendar
         get() {
             val currentAccount = authService.googleSignInUser

             if (_calendarService == null ||
                 currentAccount != lastUsedAccount ||
                 currentAccount?.name?.compareTo(
                     lastUsedAccount?.name ?: "",
                     ignoreCase = true
                 ) != 0
             ) {
                 lastUsedAccount = currentAccount

                 val credential = GoogleAccountCredential.usingOAuth2(
                     context,
                     listOf(CalendarScopes.CALENDAR_READONLY)
                 ).setBackOff(ExponentialBackOff())
                 credential.selectedAccount = currentAccount

                 _calendarService = Calendar.Builder(
                     NetHttpTransport(),
                     JacksonFactory.getDefaultInstance(),
                     credential
                 ).setApplicationName(context.getString(R.string.app_name)).build()
             }

             return _calendarService!!
         }*/

    companion object {
        val TAG = GoogleCalendarService::class.simpleName
    }


    override suspend fun getCalendarEvents(): Result<List<CalendarEvent>> =
        withContext(Dispatchers.IO) {

            try {
                val settings =
                    settingsRepository.getSettings().first()
                        .copy(lastSyncedTime = Clock.System.now().toEpochMilliseconds())
                settingsRepository.saveSettings(settings)
                alarmScheduler.cancelAllAlarms(context)
                calendarEventDao.deleteAllEvents()
                Log.d(TAG, "Fetching Google Calendar events with access token.")
                val nowMillis = Clock.System.now().toEpochMilliseconds()
                val now = DateTime(nowMillis)
                val in24HoursMillis =
                    nowMillis + ((24 * 60 * 60 * 1000) * settings.defaultDaysToSyncFromNow)
                val timeMax = DateTime(in24HoursMillis)
                val events = _calendarService!!.events().list("primary")
                    .setTimeMin(now)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .setTimeMax(timeMax)
                    .execute()

                val calendarEvents = events.items?.mapNotNull { googleEvent ->
                    val eventId = googleEvent.id ?: return@mapNotNull null
                    val eventName = googleEvent.summary ?: "(No Title)"
                    val startTimeMillis: Long?
                    val endTimeMillis: Long?
                    if (googleEvent.start.dateTime != null && googleEvent.end.dateTime != null) {
                        startTimeMillis = googleEvent.start.dateTime.value
                        endTimeMillis = googleEvent.end.dateTime.value
                    } else if (googleEvent.start.date != null && googleEvent.end.date != null) {
                        val startDateString =
                            googleEvent.start.date.toStringRfc3339().substringBefore('T')
                        val endDateString =
                            googleEvent.end.date.toStringRfc3339().substringBefore('T')

                        startTimeMillis = kotlinx.datetime.LocalDate.parse(startDateString)
                            .atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
                        endTimeMillis = kotlinx.datetime.LocalDate.parse(endDateString)
                            .atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
                    } else {
                        Log.w(
                            TAG,
                            "Event '$eventName' (ID: $eventId) has no valid start/end time or date.",
                        )
                        return@mapNotNull null
                    }

                    if (startTimeMillis == null || endTimeMillis == null) {
                        Log.w(
                            TAG,
                            "Failed to parse start/end time for event '$eventName' (ID: $eventId).",
                        )
                        return@mapNotNull null
                    }

                    val reminderTimeMillis =
                        startTimeMillis - (settings.defaultDelayBeforeTriggerMinutes * 60 * 1000)
                    if (reminderTimeMillis <= Clock.System.now().toEpochMilliseconds()) {
                        Log.w(
                            TAG,
                            "Cannot schedule alarm for event ${googleEvent.id}, reminder time ($reminderTimeMillis) is in the past.",
                        )
                        return@mapNotNull null
                    }

                    val isRecurring = !googleEvent.recurringEventId.isNullOrBlank()

                    val attendeesList: List<AttendeeData>? =
                        googleEvent.attendees?.map { googleAttendee ->
                            AttendeeData(
                                id = googleAttendee.id,
                                email = googleAttendee.email,
                                displayName = googleAttendee.displayName,
                                organizer = googleAttendee.organizer
                                            ?: false,
                                self = googleAttendee.self
                                       ?: false,
                                resource = googleAttendee.resource
                                           ?: false,
                                optional = googleAttendee.optional
                                           ?: false,
                                responseStatus = AttendeeResponseStatus.fromString(
                                    googleAttendee.responseStatus,
                                ),
                                comment = googleAttendee.comment,
                                additionalGuests = googleAttendee.additionalGuests,
                            )
                        }

                    val mappedEvent = CalendarEvent(
                        eventId = eventId,
                        eventName = eventName,
                        startTimeMillis = startTimeMillis,
                        endTimeMillis = endTimeMillis,
                        isRecurring = isRecurring,
                        eventStatus = EventStatus.PENDING,
                        googleCalendarApiStatus = googleEvent.status,
                        location = googleEvent.location,
                        calendarId = googleEvent.organizer?.email,
                        notes = googleEvent.description,
                        lastUpdated = Clock.System.now().toEpochMilliseconds(),
                        attendees = attendeesList ?: emptyList(),
                    )

                    mappedEvent
                } ?: emptyList()

                Log.d(
                    TAG,
                    "Fetched ${calendarEvents.size} events. Saving to Room DB.",
                )
                if (calendarEvents.isNotEmpty()) {
                    calendarEventDao.upsertListByEventId(calendarEvents)
                }

                Result.success(calendarEvents)
            } catch (e: GoogleJsonResponseException) {
                val errorBody = e.details?.message ?: e.message ?: "Unknown API Error"
                println("$TAG Google API Error: ${e.statusCode} - $errorBody")
                println("Full exception details: $e")
                Result.failure(e)
            } catch (e: IOException) {
                println("$TAG Network Error: ${e.message} $e")
                Result.failure(e)
            } catch (e: Exception) {
                println("$TAG Unexpected Error: ${e.message} $e")
                Result.failure(e)
            }
        }

    override fun getLocalCalendarEvents(): Flow<List<CalendarEvent>> {
        return calendarEventDao.getAllEvents()
    }
}