package com.sevenspan.calarm.app.features.splash.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sevenspan.calarm.app.core.service.AuthService
import com.sevenspan.calarm.app.core.service.WorkScheduler
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class SplashViewModel(
    private val authService: AuthService,
    private val workScheduler: WorkScheduler,
) : ViewModel() {
    private val _uiEvent = Channel<SplashEvent>()
    val uiEvent: Flow<SplashEvent> = _uiEvent.receiveAsFlow()

    init {
        viewModelScope.launch {
            val isLoggedIn = authService.isUserSignedIn()
            if (isLoggedIn) {
                _uiEvent.send(SplashEvent.NavigateToHome)
            } else {
                workScheduler.cancelWorker()
                _uiEvent.send(SplashEvent.NavigateToSignIn)
            }
            /*val baseUrl = authService.fetchBaseUrlFromFirebase()
            if (!baseUrl.isNullOrBlank()) {
                initRetrofit(baseUrl)

            }*/

        }
    }
}

sealed class SplashEvent {
    object NavigateToSignIn : SplashEvent()
    object NavigateToHome : SplashEvent()
}
