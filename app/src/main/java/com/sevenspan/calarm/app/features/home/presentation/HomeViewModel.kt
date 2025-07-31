package com.sevenspan.calarm.app.features.home.presentation

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meticha.triggerx.TriggerXAlarmScheduler
import com.sevenspan.calarm.app.core.data.repository.SettingsRepository
import com.sevenspan.calarm.app.core.model.CalendarEvent
import com.sevenspan.calarm.app.core.service.SubscriptionService
import com.sevenspan.calarm.app.core.service.WorkScheduler
import com.sevenspan.calarm.app.features.home.data.repository.HomeRepository
import com.sevenspan.calarm.app.features.signin.domain.repository.SignInRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val homeRepository: HomeRepository,
    private val settingsRepository: SettingsRepository,
    private val signInRepository: SignInRepository,
    private val alarmScheduler: TriggerXAlarmScheduler,
    private val workScheduler: WorkScheduler,
    private val subscriptionService: SubscriptionService,
) : ViewModel() {
    private val _state = MutableStateFlow(HomeState())

    val state: StateFlow<HomeState> = _state.asStateFlow()
    private val _uiEvent = Channel<HomeUIEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        viewModelScope.launch {
            workScheduler.scheduleWorker()

            if (!subscriptionService.isPremiumUser()) {
                _uiEvent.send(HomeUIEvent.SubscriptionEndedEvent)
            }
        }
        notifyUserInfo()
    }

    /*init {
        viewModelScope.launch {
            while (true) {
                val appSettings = settingsRepository.getSettings().first()
                _state.update {
                    it.copy(
                        lastSynced = appSettings.lastSyncedTime,
                        tick = System.currentTimeMillis(),
                    )
                }
                delay(60_000L)
            }
        }
    }*/

    fun getCalendar(context: Context) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    status = HomeStatus.LOADING,
                )
            }
            homeRepository.getCalendarEventsFromGoogle().fold(
                onFailure = { error ->
                    Log.e(
                        "Calendar",
                        error.toString(),
                    )
                    _state.update {
                        it.copy(
                            status = HomeStatus.ERROR,
                            error = error.message,
                        )
                    }
                },
                onSuccess = { events ->
                    val appSettings = settingsRepository.getSettings().first()
                    val calendarEvents =
                        homeRepository.getLocalCalendarEvents().firstOrNull() ?: emptyList()
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
                                event.startTimeMillis - (appSettings.defaultDelayBeforeTriggerMinutes * 60 * 1000)
                            if (reminderTimeMillis > System.currentTimeMillis()) {
                                Pair(event.id, reminderTimeMillis)
                            } else if (event.startTimeMillis > System.currentTimeMillis()) {
                                Pair(event.id, event.startTimeMillis)
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
                            _uiEvent.send(HomeUIEvent.ScheduledEvent)
                        }
                    }
                },
            )
        }
    }

    fun switchAccount(context: Activity) {
        viewModelScope.launch {
            signInRepository.signIn(context).onSuccess {
                val intent = signInRepository.getGoogleSignInIntent(context)
                if (intent != null) {
                    _uiEvent.send(HomeUIEvent.OnSignInIntentGenerated(intent))
                } else {
                    signInRepository.subscribeToCalendarWebhook()
                        .onSuccess {
                            notifyUserInfo()
                            getCalendar(context)
                        }
                        .onFailure {
                            workScheduler.cancelWorker()
                            _uiEvent.send(HomeUIEvent.OnSignInFailure)
                        }
                }
            }
        }
    }

    private fun notifyUserInfo() {
        _state.update {
            it.copy(
                userData = homeRepository.getUserData(),
            )
        }
    }

    fun processResult(context: Activity, result: ActivityResult) {
        viewModelScope.launch {
            signInRepository.processAuthCode(context, result = result)
                .onSuccess {
                    notifyUserInfo()
                    getCalendar(context)
                }
                .onFailure {
                    workScheduler.cancelWorker()
                    _uiEvent.send(HomeUIEvent.OnSignInFailure)
                }
        }
    }

    suspend fun isSubscriptionActive(): Boolean = subscriptionService.isPremiumUser()

    override fun onCleared() {
        super.onCleared()
        _uiEvent.close()
    }
}

sealed interface HomeUIEvent {
    data object None : HomeUIEvent
    data object ScheduledEvent : HomeUIEvent
    data object OnSignInFailure : HomeUIEvent
    data class OnSignInIntentGenerated(val intent: PendingIntent?) : HomeUIEvent
    data class LocalCalendarFetchedEvent(val events: List<CalendarEvent>) : HomeUIEvent
    data object SubscriptionEndedEvent : HomeUIEvent
}
