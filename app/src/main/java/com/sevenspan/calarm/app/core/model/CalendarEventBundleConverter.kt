package com.sevenspan.calarm.app.core.model // Or your preferred package

import android.os.Bundle
import android.util.Log
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

object CalendarEventBundleConverter {

    private const val TAG = "CalendarEventBundleConv"

    private const val KEY_EVENT_ID = "event_id"
    private const val KEY_EVENT_NAME = "event_name"
    private const val KEY_START_TIME_MILLIS = "start_time_millis"
    private const val KEY_END_TIME_MILLIS = "end_time_millis"
    private const val KEY_IS_RECURRING = "is_recurring"
    private const val KEY_GOOGLE_CALENDAR_API_STATUS = "google_calendar_api_status"
    private const val KEY_EVENT_STATUS = "event_status"
    private const val KEY_LOCATION = "location"
    private const val KEY_CALENDAR_ID = "calendar_id"
    private const val KEY_LAST_UPDATED = "last_updated"
    private const val KEY_NOTES = "notes"
    private const val KEY_ATTENDEES_JSON = "attendees_json"

    fun CalendarEvent.toBundle(): Bundle {
        val bundle = Bundle()
        bundle.putString(KEY_EVENT_ID, this.eventId)
        bundle.putString(KEY_EVENT_NAME, this.eventName)
        bundle.putLong(KEY_START_TIME_MILLIS, this.startTimeMillis)
        bundle.putLong(KEY_END_TIME_MILLIS, this.endTimeMillis)
        bundle.putBoolean(KEY_IS_RECURRING, this.isRecurring)
        bundle.putString(KEY_GOOGLE_CALENDAR_API_STATUS, this.googleCalendarApiStatus)
        bundle.putString(KEY_EVENT_STATUS, this.eventStatus.name)
        bundle.putString(KEY_LOCATION, this.location)
        bundle.putString(KEY_CALENDAR_ID, this.calendarId)
        bundle.putLong(KEY_LAST_UPDATED, this.lastUpdated)
        bundle.putString(KEY_NOTES, this.notes)

        this.attendees?.let {
            if (it.isNotEmpty()) {
                try {
                    val attendeesJson = Json.encodeToString(it)
                    bundle.putString(KEY_ATTENDEES_JSON, attendeesJson)
                } catch (e: SerializationException) {
                    Log.e(TAG, "Error serializing attendees to JSON", e)
                    // Handle error, maybe put nothing or an error marker
                }
            }
        }
        return bundle
    }

    fun Bundle.toCalendarEvent(): CalendarEvent? {
        val eventId = this.getString(KEY_EVENT_ID)
        val eventName = this.getString(KEY_EVENT_NAME)

        if (eventId == null || eventName == null) {
            Log.e(TAG, "Required fields eventId or eventName are missing in Bundle.")
            return null // Or throw an IllegalArgumentException
        }

        val startTimeMillis = this.getLong(KEY_START_TIME_MILLIS)
        val endTimeMillis = this.getLong(KEY_END_TIME_MILLIS)
        val isRecurring = this.getBoolean(KEY_IS_RECURRING, false)
        val googleCalendarApiStatus = this.getString(KEY_GOOGLE_CALENDAR_API_STATUS)
        val eventStatusString = this.getString(KEY_EVENT_STATUS)
        val location = this.getString(KEY_LOCATION)
        val calendarId = this.getString(KEY_CALENDAR_ID)
        val lastUpdated = this.getLong(KEY_LAST_UPDATED, System.currentTimeMillis())
        val notes = this.getString(KEY_NOTES)

        val eventStatus = try {
            eventStatusString?.let { EventStatus.valueOf(it) } ?: EventStatus.PENDING
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Invalid EventStatus string in Bundle: $eventStatusString", e)
            EventStatus.PENDING // Default or error state
        }

        val attendeesJson = this.getString(KEY_ATTENDEES_JSON)
        val attendees: List<AttendeeData>? = attendeesJson?.let {
            try {
                Json.decodeFromString<List<AttendeeData>>(it)
            } catch (e: SerializationException) {
                Log.e(TAG, "Error deserializing attendees from JSON", e)
                null // Or emptyList() depending on desired behavior
            }
        }

        return CalendarEvent(
            eventId = eventId,
            eventName = eventName,
            startTimeMillis = startTimeMillis,
            endTimeMillis = endTimeMillis,
            isRecurring = isRecurring,
            googleCalendarApiStatus = googleCalendarApiStatus,
            eventStatus = eventStatus,
            location = location,
            calendarId = calendarId,
            lastUpdated = lastUpdated,
            notes = notes,
            attendees = attendees,
        )
    }
}
