package com.sevenspan.calarm.app.core.data.model

import com.sevenspan.calarm.app.core.model.AttendeeData
import com.sevenspan.calarm.app.core.model.EventStatus
import kotlinx.serialization.Serializable

@Serializable
data class CalendarEventData(
    val id: Int = 0,
    val eventId: String,
    val eventName: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val isRecurring: Boolean = false,
    val googleCalendarApiStatus: String? = null,
//    val eventStatus: EventStatus = EventStatus.PENDING,
    val location: String? = null,
    val calendarId: String? = null,
    val lastUpdated: Long = System.currentTimeMillis(),
    val notes: String? = null,
    val attendees: List<AttendeeData>? = null,
)

fun CalendarEventData.toRoomModel(): com.sevenspan.calarm.app.core.model.CalendarEvent =
    com.sevenspan.calarm.app.core.model.CalendarEvent(
        eventId = this.eventId,
        eventName = this.eventName,
        startTimeMillis = this.startTimeMillis,
        endTimeMillis = this.endTimeMillis,
        isRecurring = this.isRecurring,
        googleCalendarApiStatus = this.googleCalendarApiStatus,
        eventStatus = EventStatus.PENDING,
        location = this.location,
        calendarId = this.calendarId,
        lastUpdated = this.lastUpdated,
        notes = this.notes,
        attendees = this.attendees,
    )

fun List<CalendarEventData>.toRoomList(): List<com.sevenspan.calarm.app.core.model.CalendarEvent> =
    this.map { it.toRoomModel() }
