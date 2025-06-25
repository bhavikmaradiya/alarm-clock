package com.bhavikm.calarm.app.core.service

import android.content.Context
import android.util.Log
import com.bhavikm.calarm.R
import com.bhavikm.calarm.app.core.data.source.local.AppSettingsDao
import com.bhavikm.calarm.app.core.data.source.local.CalendarEventDao
import com.bhavikm.calarm.app.core.model.AttendeeData
import com.bhavikm.calarm.app.core.model.AttendeeResponseStatus
import com.bhavikm.calarm.app.core.model.CalendarEvent
import com.bhavikm.calarm.app.core.model.EventStatus
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.DateTime
import com.google.api.client.util.ExponentialBackOff
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.CalendarScopes
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
    private val appSettingsDao: AppSettingsDao,
    val alarmScheduler: TriggerXAlarmScheduler,
    private val authService: AuthService,
) : CalendarService {

    private val calendarService: Calendar

    companion object {
        val TAG = GoogleCalendarService::class.simpleName
    }

    init {
        val mCredential = GoogleAccountCredential.usingOAuth2(
            context,
            arrayListOf(CalendarScopes.CALENDAR_READONLY),
        )
            .setBackOff(ExponentialBackOff())
        mCredential.selectedAccount =
            authService.googleSignInUser
        val transport = NetHttpTransport()
        val jsonFactory = JacksonFactory.getDefaultInstance()
        calendarService = Calendar.Builder(transport, jsonFactory, mCredential)
            .setApplicationName(context.getString(R.string.app_name))
            .build()
    }

    override suspend fun getCalendarEvents(): Result<List<CalendarEvent>> =
        withContext(Dispatchers.IO) {
            try {
                val userId = requireNotNull(authService.currentUser?.uid)
                val settings =
                    appSettingsDao.getSettings(userId).first()
                        .copy(lastSyncedTime = Clock.System.now().toEpochMilliseconds())
                appSettingsDao.upsertSettings(settings)
                alarmScheduler.cancelAllAlarms(context)
                calendarEventDao.deleteAllEvents()
                Log.d(TAG, "Fetching Google Calendar events with access token.")
                val nowMillis = Clock.System.now().toEpochMilliseconds()
                val now = DateTime(nowMillis)
                val in24HoursMillis =
                    nowMillis + ((24 * 60 * 60 * 1000) * settings.defaultDaysToSyncFromNow)
                val timeMax = DateTime(in24HoursMillis)
                val events = calendarService.events().list("primary")
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
                                ), // Uses your enum's fromString method
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