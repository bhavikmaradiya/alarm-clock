package com.example.workmanager.app.core.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calendar_events")
data class CalendarEvent(
    @PrimaryKey val eventId: String,
    val eventName: String,
    val isCancelled: Boolean = false,
    val hasAlarmScheduled: Boolean = false,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val isRecurring: Boolean = false,
    val eventStatus: EventStatus = EventStatus.PENDING, // NEW FIELD
    val location: String? = null,
    val calendarId: String? = null,
    val lastUpdated: Long = System.currentTimeMillis(),
    val notes: String? = null,
)

enum class EventStatus {
    PENDING,    // Not yet triggered
    CANCELLED,    // Not yet triggered
    COMPLETED,  // User completed
}
