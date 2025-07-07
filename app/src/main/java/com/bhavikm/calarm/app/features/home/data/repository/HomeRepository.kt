package com.bhavikm.calarm.app.features.home.data.repository

import com.bhavikm.calarm.app.core.model.CalendarEvent
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow

interface HomeRepository {
    /**
     * Fetches calendar events from the Google Calendar API for the given access token,
     * parses them, saves them to the local Room database, and returns the fetched list.
     *
     * @param accessToken The OAuth2 access token for Google Calendar API.
     * @return Either a String error message on failure or a List of [com.bhavikm.calarm.app.core.model.CalendarEvent] on success.
     */
    suspend fun getCalendarEventsFromGoogle(): Result<List<CalendarEvent>>

    /**
     * Retrieves all locally stored [CalendarEvent]s from the Room database as a Flow.
     * The Flow will emit a new list whenever the underlying data changes.
     *
     * @return A Flow emitting a list of [CalendarEvent].
     */
    fun getLocalCalendarEvents(): Flow<List<CalendarEvent>>
    fun getUserData(): FirebaseUser?
}
