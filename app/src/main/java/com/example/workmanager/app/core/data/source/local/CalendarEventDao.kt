package com.example.workmanager.app.core.data.source.local

import androidx.room.*
import com.example.workmanager.app.core.domain.model.CalendarEvent
import com.example.workmanager.app.core.domain.model.EventStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface CalendarEventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEvent(event: CalendarEvent)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEvents(events: List<CalendarEvent>)

    @Delete
    suspend fun deleteEvent(event: CalendarEvent)

    @Query("DELETE FROM calendar_events")
    suspend fun deleteAllEvents()

    @Query("DELETE FROM calendar_events WHERE eventId = :eventId")
    suspend fun deleteEventById(eventId: String)

    @Query("SELECT * FROM calendar_events WHERE eventId = :eventId LIMIT 1")
    suspend fun getEventById(eventId: String): CalendarEvent?

    @Query("SELECT * FROM calendar_events WHERE id = :eventId LIMIT 1")
    suspend fun getEventById(eventId: Int): CalendarEvent?

    @Query("SELECT * FROM calendar_events ORDER BY startTimeMillis ASC")
    fun getAllEvents(): Flow<List<CalendarEvent>>

    @Query("SELECT * FROM calendar_events WHERE startTimeMillis > :currentTime ORDER BY startTimeMillis ASC")
    fun getUpcomingEvents(currentTime: Long = System.currentTimeMillis()): Flow<List<CalendarEvent>>

    @Query("SELECT * FROM calendar_events WHERE eventStatus = :status AND startTimeMillis > :currentTime ORDER BY startTimeMillis ASC")
    suspend fun getEventsByStatus(
        status: EventStatus,
        currentTime: Long = System.currentTimeMillis(),
    ): List<CalendarEvent>

    @Query("UPDATE calendar_events SET eventStatus = :status WHERE eventId = :eventId")
    suspend fun updateEventStatus(eventId: String, status: EventStatus)

    @Query("SELECT * FROM calendar_events WHERE isRecurring = 0 AND eventStatus = :status ORDER BY startTimeMillis ASC")
    suspend fun getNonRecurringEventsByStatus(status: EventStatus): List<CalendarEvent>

    @Query("DELETE FROM calendar_events WHERE endTimeMillis < :thresholdTime")
    suspend fun clearPastEvents(thresholdTime: Long = System.currentTimeMillis())
}
