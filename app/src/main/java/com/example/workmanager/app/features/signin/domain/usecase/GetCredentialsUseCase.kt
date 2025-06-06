package com.example.workmanager.app.features.signin.domain.usecase

import android.app.Activity
import android.content.Context
import androidx.credentials.GetCredentialResponse
import arrow.core.Either
import com.example.workmanager.app.features.signin.data.repository.SignInError
import com.example.workmanager.app.features.signin.domain.repository.SignInRepository
import com.google.firebase.auth.FirebaseUser

class GetCredentialsUseCase(private val signInRepository: SignInRepository) {
    suspend operator fun invoke(context: Activity): Either<SignInError, FirebaseUser> {
        return signInRepository.getCredentials(context)
    }
}