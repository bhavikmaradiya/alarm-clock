package com.example.workmanager.app.features.home.data.repository

import android.content.Context
import android.util.Log
import arrow.core.Either
import arrow.core.raise.result
import com.example.workmanager.app.core.data.source.local.CalendarEventDao
import com.example.workmanager.app.core.domain.model.AttendeeData
import com.example.workmanager.app.core.domain.model.AttendeeResponseStatus
import com.example.workmanager.app.core.domain.model.CalendarEvent
import com.example.workmanager.app.core.domain.model.EventStatus
import com.example.workmanager.app.features.home.domain.repository.HomeRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.util.DateTime
import com.google.api.client.util.ExponentialBackOff
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.CalendarScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone // For all-day events

class HomeRepositoryImpl(
    private val context: Context,
    private val calendarEventDao: CalendarEventDao, // DAO for Room database
) : HomeRepository {


    private val calendarService: Calendar

    init {
        val mCredential = GoogleAccountCredential.usingOAuth2(
            context,
            arrayListOf(CalendarScopes.CALENDAR)
        )
            .setBackOff(ExponentialBackOff())
        mCredential.selectedAccount =
            GoogleSignIn.getLastSignedInAccount(context)?.account
        val transport = com.google.api.client.http.javanet.NetHttpTransport()
        val jsonFactory = com.google.api.client.json.jackson2.JacksonFactory.getDefaultInstance()
        calendarService = Calendar.Builder(transport, jsonFactory, mCredential)
            .setApplicationName("WorkManager")
            .build()
    }


    private val allDayEventDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC") // Google Calendar all-day events are date-only
    }

    override suspend fun getCalendarEventsFromGoogle(): Either<String, List<CalendarEvent>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("HomeRepositoryImpl", "Fetching Google Calendar events with access token.")

                val nowMillis = System.currentTimeMillis()
                val now = DateTime(nowMillis)
                val in24HoursMillis = nowMillis + ((24 * 60 * 60 * 1000) * 5) // 24 hours in milliseconds
                val timeMax = DateTime(in24HoursMillis)
                val events = calendarService.events().list("primary")
                    .setTimeMin(now)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .setTimeMax(timeMax)
                    .execute()

                val calendarEvents = events.items?.mapNotNull { googleEvent ->
                    val eventId = googleEvent.id ?: return@mapNotNull null // Skip if no ID
                    val eventName = googleEvent.summary ?: "(No Title)"
                    val startTimeMillis: Long?
                    val endTimeMillis: Long?
                    if (googleEvent.start.dateTime != null && googleEvent.end.dateTime != null) {
                        startTimeMillis = googleEvent.start.dateTime.value
                        endTimeMillis = googleEvent.end.dateTime.value
                    } else if (googleEvent.start.date != null && googleEvent.end.date != null) {
                        startTimeMillis = googleEvent.start.date?.value
                        endTimeMillis = googleEvent.end.date?.value
                    } else {
                        Log.w(
                            "HomeRepositoryImpl",
                            "Event '$eventName' (ID: $eventId) has no valid start/end time or date."
                        )
                        return@mapNotNull null // Skip if no valid date/time
                    }

                    if (startTimeMillis == null || endTimeMillis == null) {
                        Log.w(
                            "HomeRepositoryImpl",
                            "Failed to parse start/end time for event '$eventName' (ID: $eventId)."
                        )
                        return@mapNotNull null
                    }

                    val reminderTimeMillis = startTimeMillis - (10 * 60 * 1000)
                    if (reminderTimeMillis <= System.currentTimeMillis()) {
                        Log.w(
                            "HomeRepositoryImpl",
                            "Cannot schedule alarm for event ${googleEvent.id}, reminder time ($reminderTimeMillis) is in the past."
                        )
                        return@mapNotNull null // Indicate failure to schedule because it's too late
                    }

                    val isRecurring = !googleEvent.recurringEventId.isNullOrBlank()

                    val attendeesList: List<AttendeeData>? =
                        googleEvent.attendees?.map { googleAttendee ->
                            // googleAttendee is of type com.google.api.services.calendar.model.EventAttendee
                            AttendeeData(
                                id = googleAttendee.id, // Maps to googleAttendee.getId()
                                email = googleAttendee.email, // Maps to googleAttendee.getEmail()
                                displayName = googleAttendee.displayName, // Maps to googleAttendee.getDisplayName()
                                organizer = googleAttendee.organizer, // Maps to googleAttendee.getOrganizer() (Boolean?)
                                self = googleAttendee.self, // Maps to googleAttendee.getSelf() (Boolean?)
                                resource = googleAttendee.resource, // Maps to googleAttendee.getResource() (Boolean?)
                                optional = googleAttendee.optional, // Maps to googleAttendee.getOptional() (Boolean?)
                                responseStatus = AttendeeResponseStatus.fromString(googleAttendee.responseStatus), // Uses your enum's fromString method
                                comment = googleAttendee.comment, // Maps to googleAttendee.getComment()
                                additionalGuests = googleAttendee.additionalGuests // Maps to googleAttendee.getAdditionalGuests() (Int?)
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
                        calendarId = googleEvent.organizer?.email, // Using organizer's email as a proxy for calendar ID
                        notes = googleEvent.description,
                        lastUpdated = System.currentTimeMillis(),
                        attendees = attendeesList ?: emptyList()
                    )

                    Log.d(
                        "HomeRepositoryImpl", "Mapped Event: \n" +
                                              "  ID: ${mappedEvent.eventId}\n" +
                                              "  Name: ${mappedEvent.eventName}\n" +
                                              "  Start: ${Date(mappedEvent.startTimeMillis)}\n" +
                                              "  End: ${Date(mappedEvent.endTimeMillis)}\n" +
                                              "  Google API Status: ${mappedEvent.googleCalendarApiStatus}\n" +
                                              "  Local App Status: ${mappedEvent.eventStatus}\n" +
                                              "  Recurring: ${mappedEvent.isRecurring}\n" +
                                              "  Location: ${mappedEvent.location}\n" +
                                              "  Calendar ID: ${mappedEvent.calendarId}\n" +
                                              "  Attendees: ${mappedEvent.attendees?.size ?: 0}\n" +
                                              "  Last Updated: ${Date(mappedEvent.lastUpdated)}"
                    )

                    mappedEvent
                } ?: emptyList()

                Log.d(
                    "HomeRepositoryImpl",
                    "Fetched ${calendarEvents.size} events. Saving to Room DB."
                )
                if (calendarEvents.isNotEmpty()) {
                    calendarEventDao.upsertEvents(calendarEvents)
                }



                Either.Right(calendarEvents)

            } catch (e: GoogleJsonResponseException) {
                val errorBody = e.details?.message ?: e.message ?: "Unknown API Error"
                println("HomeRepositoryImpl Google API Error: ${e.statusCode} - $errorBody $e")
                Either.Left("API Error: ${e.statusCode} - $errorBody")
            } catch (e: IOException) {
                println("HomeRepositoryImpl Network Error: ${e.message} $e")
                Either.Left("Network Error: ${e.message}")
            } catch (e: Exception) {
                println("HomeRepositoryImpl Unexpected Error: ${e.message} $e")
                Either.Left("An unexpected error occurred: ${e.message}")
            }
        }
    }

    override fun getLocalCalendarEvents(): Flow<List<CalendarEvent>> {
        Log.d("HomeRepositoryImpl", "Providing Flow for local calendar events from Room DB.")
        return calendarEventDao.getAllEvents() // Assumes this method exists in your DAO
    }
}
