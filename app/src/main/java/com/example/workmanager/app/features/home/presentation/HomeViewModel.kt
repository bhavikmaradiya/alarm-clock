package com.example.workmanager.app.features.home.presentation

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workmanager.app.features.home.domain.repository.HomeRepository
import com.meticha.triggerx.TriggerXAlarmScheduler
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

class HomeViewModel(
    val repository: HomeRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(HomeState())

    /*val state =
        repository
            .getLocalCalendarEvents()
            .map {
                _state.update { state ->
                    state.copy(
                        events = it,
                        status = if (it.isNotEmpty()) HomeStatus.LOADED else HomeStatus.EMPTY,
                    )
                }
                _state.value
            }.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                HomeState(),
            )*/
    val state = _state.asStateFlow()
    private val _uiEvent = MutableSharedFlow<HomeUIEvent>()
    val uiEvent = _uiEvent.asSharedFlow()


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
                    val event = calendarEvents.firstOrNull()
                    if (event != null) {
                        val inFiveMinutes = Calendar.getInstance().apply {
                            add(Calendar.MINUTE, 1)
                        }.timeInMillis
                        TriggerXAlarmScheduler().scheduleAlarm(
                            context = context,
                            triggerAtMillis = inFiveMinutes,
                            type = event.eventId,
                        )
                        println("Event scheduled for $inFiveMinutes for event: ${event.eventName}")
                    }
                }
            })
        }
    }
}

sealed interface HomeUIEvent {
    data object None : HomeUIEvent

}
