package com.bhavikm.calarm.app.features.signin.presentation

import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bhavikm.calarm.app.features.signin.domain.repository.SignInRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SignInViewModel(private val signInRepository: SignInRepository) : ViewModel() {
    private val _state = MutableStateFlow(SignInState())
    val state: StateFlow<SignInState> = _state.asStateFlow()

    private val _uiEvent = Channel<SignInEvent>()
    val uiEvent: Flow<SignInEvent> = _uiEvent.receiveAsFlow()

    fun signIn(context: Activity) {
        _state.update { it.copy(status = SignInStatus.LOADING) }
        viewModelScope.launch {
            signInRepository.signIn(context).fold(
                onFailure = {
                    _state.update { state ->
                        state.copy(status = SignInStatus.ERROR, error = it.message)
                    }
                },
                onSuccess = {
                    val intent = signInRepository.getGoogleSignInIntent(context)
                    _uiEvent.send(SignInEvent.OnSignInIntentGenerated(intent))
                },
            )
        }
    }

    fun processResult(activity: Activity, result: ActivityResult) {
        viewModelScope.launch {
            signInRepository.processAuthCode(activity, result).fold(
                onFailure = {
                    _state.update { state ->
                        state.copy(
                            status = SignInStatus.ERROR,
                            error = it.message,
                        )
                    }
                },
                onSuccess = {
                    _state.update { state ->
                        state.copy(status = SignInStatus.SUCCESS, userData = it)
                    }
                },
            )
        }
    }

    fun processResult(result: ActivityResult) {
        viewModelScope.launch {
            signInRepository.processAuthCode(result).fold(
                onFailure = {
//                    signInRepository.signOut()
                    _state.update { state ->
                        state.copy(
                            status = SignInStatus.ERROR,
                            error = it.message,
                        )
                    }
                },
                onSuccess = {
                    _state.update { state ->
                        state.copy(
                            status = SignInStatus.SUCCESS,
                            userData = it
                        )
                    }
                },
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        _uiEvent.close()
    }
}

sealed interface SignInEvent {
    data object None : SignInEvent
    data class OnSignInIntentGenerated(val intent: Intent) : SignInEvent
}
