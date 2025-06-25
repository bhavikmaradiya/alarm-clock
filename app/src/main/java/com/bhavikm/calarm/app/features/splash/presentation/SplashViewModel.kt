package com.bhavikm.calarm.app.features.splash.presentation

import androidx.lifecycle.ViewModel
import com.bhavikm.calarm.app.core.service.AuthService

class SplashViewModel(
    private val authService: AuthService,
) : ViewModel() {

    fun isUserLoggedIn(): Boolean = authService.isUserSignedIn
}
