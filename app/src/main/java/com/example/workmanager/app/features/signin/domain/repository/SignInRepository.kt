package com.example.workmanager.app.features.signin.domain.repository

import android.app.Activity
import android.content.Context
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import arrow.core.Either
import com.example.workmanager.app.features.signin.data.repository.SignInError
import com.google.firebase.auth.FirebaseUser

interface SignInRepository {
    suspend fun getCredentials(context: Activity): Either<SignInError, FirebaseUser>
}