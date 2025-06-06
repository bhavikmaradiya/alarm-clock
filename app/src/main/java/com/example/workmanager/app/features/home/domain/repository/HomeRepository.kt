package com.example.workmanager.app.features.home.domain.repository

import arrow.core.Either
import com.example.workmanager.app.core.domain.model.CalendarEvent
import kotlinx.coroutines.flow.Flow

interface HomeRepository {
    /**
     * Fetches calendar events from the Google Calendar API for the given access token,
     * parses them, saves them to the local Room database, and returns the fetched list.
     *
     * @param accessToken The OAuth2 access token for Google Calendar API.
     * @return Either a String error message on failure or a List of [CalendarEvent] on success.
     */
    suspend fun getCalendarEventsFromGoogle(): Either<String, List<CalendarEvent>>

    /**
     * Retrieves all locally stored [CalendarEvent]s from the Room database as a Flow.
     * The Flow will emit a new list whenever the underlying data changes.
     *
     * @return A Flow emitting a list of [CalendarEvent].
     */
    fun getLocalCalendarEvents(): Flow<List<CalendarEvent>>
}
