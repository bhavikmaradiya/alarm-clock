package com.sevenspan.calarm.app.core.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
enum class AttendeeResponseStatus {
    NEEDS_ACTION,
    DECLINED,
    TENTATIVE,
    ACCEPTED,
    UNKNOWN, // Fallback for any unexpected status strings
    ;

    companion object {
        fun fromString(status: String?): AttendeeResponseStatus = when (status?.lowercase()) {
            "needsaction" -> NEEDS_ACTION
            "declined"    -> DECLINED
            "tentative"   -> TENTATIVE
            "accepted"    -> ACCEPTED
            else          -> UNKNOWN
        }
    }
}

// Updated data claÏss for storing detailed attendee info
@Serializable // Make it serializable for JSON conversion
data class AttendeeData(
    val id: String?, // Attendee ID
    val email: String?,
    val displayName: String?,
    val organizer: Boolean?,
    val self: Boolean?,
    val resource: Boolean?,
    val optional: Boolean?,
    val responseStatus: AttendeeResponseStatus?, // Using the enum
    val comment: String?,
    val additionalGuests: Int?,
)

@Entity(
    tableName = "calendar_events",
    indices = [Index(value = ["eventId"], unique = true)],
)
data class CalendarEvent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val eventId: String,
    val eventName: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val isRecurring: Boolean = false,
    val googleCalendarApiStatus: String? = null, // Stores raw status from Google Calendar API
    val eventStatus: EventStatus = EventStatus.PENDING, // App's local status for alarm/notification
    val location: String? = null,
    val calendarId: String? = null,
    val lastUpdated: Long = System.currentTimeMillis(),
    val notes: String? = null,
    val attendees: List<AttendeeData>? = null,
)

enum class EventStatus {
    PENDING, // App needs to schedule an alarm/notification for this event.
    SCHEDULED, // App has successfully scheduled an alarm/notification.
    CANCELLED, // Event is considered cancelled by the app (either API or user action). No alarm.
    COMPLETED, // Event is considered completed or past by the app. No alarm.
}
