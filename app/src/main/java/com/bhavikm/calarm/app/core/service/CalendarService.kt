package com.bhavikm.calarm.app.core.service

import android.content.Context
import android.util.Log
import com.bhavikm.calarm.app.core.data.model.toRoomList
import com.bhavikm.calarm.app.core.data.repository.SettingsRepository
import com.bhavikm.calarm.app.core.data.source.local.CalendarEventDao
import com.bhavikm.calarm.app.core.model.CalendarEvent
import com.bhavikm.calarm.app.data.network.ApiClient
import com.meticha.triggerx.TriggerXAlarmScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

interface CalendarService {
    suspend fun getCalendarEvents(): Result<List<CalendarEvent>>
    fun getLocalCalendarEvents(): Flow<List<CalendarEvent>>
}

class GoogleCalendarService(
    private val context: Context,
    private val calendarEventDao: CalendarEventDao,
    private val settingsRepository: SettingsRepository,
    val alarmScheduler: TriggerXAlarmScheduler,
    private val authService: AuthService,
) : CalendarService {

    companion object {
        val TAG = GoogleCalendarService::class.simpleName
    }


    override suspend fun getCalendarEvents(): Result<List<CalendarEvent>> =
        withContext(Dispatchers.IO) {

            try {
                val settings =
                    settingsRepository.getSettings().first()
                        .copy(lastSyncedTime = Clock.System.now().toEpochMilliseconds())
                settingsRepository.saveSettings(settings)
                alarmScheduler.cancelAllAlarms(context)
                calendarEventDao.deleteAllEvents()
                Log.d(TAG, "Fetching Google Calendar events with access token.")


                val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
                val now = OffsetDateTime.now(ZoneOffset.UTC)
                val timeMax = now.plusHours((24 * settings.defaultDaysToSyncFromNow).toLong())

                val response = ApiClient.apiService
                    .getCalendarEvents(
                        timeMin = now.format(formatter),
                        timeMax = timeMax.format(formatter),
                        userId = authService.currentUser!!.uid
                    )

                val calendarList = response.body()?.toRoomList() ?: listOf()

                Log.d(
                    TAG,
                    "Fetched ${calendarList.size} events. Saving to Room DB.",
                )
                if (calendarList.isNotEmpty()) {
                    calendarEventDao.upsertListByEventId(calendarList)
                }

                Result.success(calendarList)
            } catch (e: Exception) {
                println("$TAG Unexpected Error: ${e.message} $e")
                Result.failure(e)
            }
        }

    override fun getLocalCalendarEvents(): Flow<List<CalendarEvent>> {
        return calendarEventDao.getAllEvents()
    }
}