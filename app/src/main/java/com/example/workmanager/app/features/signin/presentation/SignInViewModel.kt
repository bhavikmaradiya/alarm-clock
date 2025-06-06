package com.example.workmanager.app.features.signin.presentation

import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workmanager.app.features.signin.domain.usecase.SignInUseCases
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.calendar.CalendarScopes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SignInViewModel(val useCase: SignInUseCases) : ViewModel() {
    private val _state = MutableStateFlow(SignInState())
    val state = _state.asStateFlow()

    fun signIn(context: Activity) {
        _state.update { it.copy(status = SignInStatus.LOADING) }
        viewModelScope.launch {
            useCase.getCredentialsUseCase(context).fold(
                ifLeft = {
                    _state.update { state ->
                        state.copy(status = SignInStatus.ERROR, error = it)
                    }
                },
                ifRight = {
                    _state.update { state ->
                        state.copy(status = SignInStatus.SUCCESS, userData = it)
                    }
                },
            )

        }
    }


}