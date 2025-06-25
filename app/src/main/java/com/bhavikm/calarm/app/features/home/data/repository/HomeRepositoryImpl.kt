package com.bhavikm.calarm.app.features.home.data.repository

import android.util.Log
import com.bhavikm.calarm.app.core.model.CalendarEvent
import com.bhavikm.calarm.app.core.service.CalendarService
import kotlinx.coroutines.flow.Flow

class HomeRepositoryImpl(
    private val calendarService: CalendarService,
) : HomeRepository {


    override suspend fun getCalendarEventsFromGoogle(): Result<List<CalendarEvent>> =
        calendarService.getCalendarEvents()

    override fun getLocalCalendarEvents(): Flow<List<CalendarEvent>> {
        Log.d("HomeRepositoryImpl", "Providing Flow for local calendar events from Room DB.")
        return calendarService.getLocalCalendarEvents() // Assumes this method exists in your DAO
    }
}
