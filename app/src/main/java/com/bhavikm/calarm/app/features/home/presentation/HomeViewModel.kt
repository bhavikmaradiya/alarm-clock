package com.bhavikm.calarm.app.features.home.presentation

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bhavikm.calarm.app.core.data.repository.SettingsRepository
import com.bhavikm.calarm.app.core.model.AppSettings
import com.bhavikm.calarm.app.core.model.CalendarEvent
import com.bhavikm.calarm.app.core.service.WorkScheduler
import com.bhavikm.calarm.app.features.home.data.repository.HomeRepository
import com.meticha.triggerx.TriggerXAlarmScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: HomeRepository,
    private val settingsRepository: SettingsRepository,
    private val alarmScheduler: TriggerXAlarmScheduler,
    private val workScheduler: WorkScheduler,
) : ViewModel() {
    private val _state = MutableStateFlow(HomeState())

    val state: StateFlow<HomeState> = _state.asStateFlow()
    private val _uiEvent = MutableSharedFlow<HomeUIEvent>()
    val uiEvent: SharedFlow<HomeUIEvent> = _uiEvent.asSharedFlow()

    private lateinit var appSettings: AppSettings

    init {
        viewModelScope.launch {
            while (true) {
                appSettings = settingsRepository.getSettings().first()
                _state.update {
                    it.copy(
                        lastSynced = appSettings.lastSyncedTime,
                        tick = System.currentTimeMillis(),
                    )
                }
                delay(60_000L)
            }
        }
    }

    fun getCalendar(context: Context) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    status = HomeStatus.LOADING,
                )
            }
            repository.getCalendarEventsFromGoogle().fold(
                onFailure = { error ->
                    Log.e(
                        "Calendar",
                        error.toString(),
                    )
                    _state.update {
                        it.copy(
                            status = HomeStatus.ERROR,
                            error = error.message,
                            lastSynced = appSettings.lastSyncedTime,
                        )
                    }
                },
                onSuccess = { events ->
                    appSettings = settingsRepository.getSettings().first()
                    val calendarEvents =
                        repository.getLocalCalendarEvents().firstOrNull() ?: emptyList()
                    if (calendarEvents.isEmpty()) {
                        _state.update {
                            it.copy(
                                status = HomeStatus.EMPTY,
                                lastSynced = appSettings.lastSyncedTime,
                            )
                        }
                    } else {
                        _state.update {
                            it.copy(
                                status = HomeStatus.LOADED,
                                events = calendarEvents,
                                lastSynced = appSettings.lastSyncedTime,
                            )
                        }
                        val pairOfEvents = calendarEvents.mapNotNull { event ->
                            val reminderTimeMillis =
                                event.startTimeMillis -
                                (appSettings.defaultDelayBeforeTriggerMinutes * 60 * 1000)
                            if (reminderTimeMillis > System.currentTimeMillis()) {
                                Pair(event.id, reminderTimeMillis)
                            } else {
                                null
                            }
                        }
                        if (pairOfEvents.isNotEmpty()) {
                            alarmScheduler.scheduleAlarms(
                                context = context,
                                events = pairOfEvents,
                            )
                            println("Event scheduled for ${pairOfEvents.size} events!!")
                            _uiEvent.emit(HomeUIEvent.ScheduledEvent)
                        }
                    }
                },
            ).let {
                workScheduler.scheduleWorker()
            }
        }
    }
}

sealed interface HomeUIEvent {
    data object None : HomeUIEvent
    data object ScheduledEvent : HomeUIEvent
    data class LocalCalendarFetchedEvent(val events: List<CalendarEvent>) : HomeUIEvent
}
