package com.example.workmanager.app.features.signin.presentation

import com.example.workmanager.app.features.home.presentation.HomeStatus
import com.example.workmanager.app.features.signin.data.repository.SignInError
import com.google.firebase.auth.FirebaseUser

enum class SignInStatus {
    INITIAL,
    LOADING,
    SUCCESS,
    ERROR
}

data class SignInState(
    var status: SignInStatus = SignInStatus.INITIAL,
    var userData: FirebaseUser? = null,
    var error: SignInError? = null,

)