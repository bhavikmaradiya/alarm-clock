package com.sevenspan.calarm.app.features.signin.presentation

import com.google.firebase.auth.FirebaseUser

enum class SignInStatus {
    INITIAL,
    LOADING,
    SUCCESS,
    ERROR,
}

data class SignInState(
    var status: SignInStatus = SignInStatus.INITIAL,
    var userData: FirebaseUser? = null,
    var error: String? = null,

    )
