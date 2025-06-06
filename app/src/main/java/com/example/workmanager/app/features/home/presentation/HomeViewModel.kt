package com.example.workmanager.app.features.home.presentation

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workmanager.app.core.domain.model.CalendarEvent
import com.example.workmanager.app.features.home.domain.repository.HomeRepository
import com.meticha.triggerx.TriggerXAlarmScheduler
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    val repository: HomeRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(HomeState())

    val state = _state.asStateFlow()
    private val _uiEvent = MutableSharedFlow<HomeUIEvent>()
    val uiEvent = _uiEvent.asSharedFlow()


    init {
        viewModelScope.launch {
            repository.getLocalCalendarEvents().collect { events ->
                _uiEvent.emit(HomeUIEvent.LocalCalendarFetchedEvent(events))
                _state.update {
                    it.copy(
                        events = events,
                        status = if (events.isEmpty()) HomeStatus.EMPTY else HomeStatus.LOADED,
                    )
                }
            }
        }
    }


    fun getCalendar(context: Context) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    status = HomeStatus.LOADING
                )
            }
            repository.getCalendarEventsFromGoogle().fold(ifLeft = { error ->
                Log.e(
                    "Calendar", error.toString()
                )
                _state.update {
                    it.copy(
                        status = HomeStatus.ERROR, error = error
                    )
                }
            }, ifRight = { calendarEvents ->
                if (calendarEvents.isEmpty()) {
                    _state.update {
                        it.copy(
                            status = HomeStatus.EMPTY
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            status = HomeStatus.LOADED,
                            events = calendarEvents
                        )
                    }
                    calendarEvents.forEach { event ->
                        val reminderTimeMillis = event.startTimeMillis - (1 * 60 * 1000)
                        if (reminderTimeMillis > System.currentTimeMillis()) {
                            TriggerXAlarmScheduler().scheduleAlarm(
                                context = context,
                                triggerAtMillis = reminderTimeMillis,
                                type = event.eventId,
                            )
                            println("Event scheduled for $reminderTimeMillis for event: ${event.eventName}")
                        }
                    }
                    _uiEvent.emit(HomeUIEvent.ScheduledEvent)
                }
            })
        }
    }
}

sealed interface HomeUIEvent {
    data object None : HomeUIEvent
    data object ScheduledEvent : HomeUIEvent
    data class LocalCalendarFetchedEvent(val events: List<CalendarEvent>) : HomeUIEvent

}
