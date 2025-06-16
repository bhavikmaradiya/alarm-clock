package com.example.workmanager.app.features.home.presentation

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workmanager.app.core.domain.model.AppSettings
import com.example.workmanager.app.core.domain.model.CalendarEvent
import com.example.workmanager.app.features.home.domain.repository.HomeRepository
import com.meticha.triggerx.TriggerXAlarmScheduler
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    val repository: HomeRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(HomeState())

    val state = _state.asStateFlow()
    private val _uiEvent = MutableSharedFlow<HomeUIEvent>()
    val uiEvent = _uiEvent.asSharedFlow()
    val alarmScheduler = TriggerXAlarmScheduler()

    lateinit var appSettings: AppSettings


    init {
        /*viewModelScope.launch {
            repository.getLocalCalendarEvents().collect { events ->
                _uiEvent.emit(HomeUIEvent.LocalCalendarFetchedEvent(events))
                _state.update {
                    it.copy(
                        events = events,
                        status = if (events.isEmpty()) HomeStatus.EMPTY else HomeStatus.LOADED,
                        lastSynced = appSettings.lastSyncedTime
                    )
                }
            }
        }*/
    }


    fun getCalendar(context: Context) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    status = HomeStatus.LOADING
                )
            }
            repository.getCalendarEventsFromGoogle().fold(
                ifLeft = { error ->
                    Log.e(
                        "Calendar", error.toString()
                    )
                    _state.update {
                        it.copy(
                            status = HomeStatus.ERROR,
                            error = error,
                            lastSynced = appSettings.lastSyncedTime
                        )
                    }
                },
                ifRight = { e ->
                    appSettings = repository.getAppSettings().first()
                    val calendarEvents =
                        repository.getLocalCalendarEvents().firstOrNull() ?: emptyList()
                    if (calendarEvents.isEmpty()) {
                        _state.update {
                            it.copy(
                                status = HomeStatus.EMPTY,
                                lastSynced = appSettings.lastSyncedTime
                            )
                        }
                    } else {
                        _state.update {
                            it.copy(
                                status = HomeStatus.LOADED,
                                events = calendarEvents,
                                lastSynced = appSettings.lastSyncedTime
                            )
                        }
                        val pairOfEvents = calendarEvents.mapNotNull { event ->
                            val reminderTimeMillis = event.startTimeMillis - (1 * 60 * 1000)
                            if (reminderTimeMillis > System.currentTimeMillis()) {
                                Pair(event.id, reminderTimeMillis)
                            } else {
                                null
                            }
                        }
                        /*val pairOfEvents = calendarEvents.firstOrNull()?.let { event ->
                            val reminderTimeMillis = event.startTimeMillis - (1 * 60 * 1000)
                            if (reminderTimeMillis > System.currentTimeMillis()) {
                                Pair(event.id, reminderTimeMillis)
                            } else {
                                null
                            }
                        }*/
                        if (pairOfEvents.isNotEmpty()) {
                            alarmScheduler.scheduleAlarms(
                                context = context,
                                events = pairOfEvents
                            )
                            println("Event scheduled for ${pairOfEvents.size} events!!")
                            _uiEvent.emit(HomeUIEvent.ScheduledEvent)
                        }
                    }
                },
            )
        }
    }
}

sealed interface HomeUIEvent {
    data object None : HomeUIEvent
    data object ScheduledEvent : HomeUIEvent
    data class LocalCalendarFetchedEvent(val events: List<CalendarEvent>) : HomeUIEvent

}
