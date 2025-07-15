package com.sevenspan.calarm.app.features.splash.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sevenspan.calarm.app.core.service.AuthService
import com.sevenspan.calarm.app.data.network.ApiClient.initRetrofit
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class SplashViewModel(private val authService: AuthService) : ViewModel() {
    private val _uiEvent = Channel<SplashEvent>()
    val uiEvent: Flow<SplashEvent> = _uiEvent.receiveAsFlow()

    init {
        viewModelScope.launch {
            val baseUrl = authService.fetchBaseUrlFromFirebase()
            if (!baseUrl.isNullOrBlank()) {
                initRetrofit(baseUrl)
                val isLoggedIn = authService.isUserSignedIn()
                if (isLoggedIn) {
                    _uiEvent.send(SplashEvent.NavigateToHome)
                } else {
                    _uiEvent.send(SplashEvent.NavigateToSignIn)
                }
            }
        }
    }
}

sealed class SplashEvent {
    object NavigateToSignIn : SplashEvent()
    object NavigateToHome : SplashEvent()
}
