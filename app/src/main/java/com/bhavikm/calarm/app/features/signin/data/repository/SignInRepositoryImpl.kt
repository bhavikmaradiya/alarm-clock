package com.bhavikm.calarm.app.features.signin.data.repository

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResult
import com.bhavikm.calarm.app.core.data.source.local.AppSettingsDao
import com.bhavikm.calarm.app.core.service.AuthService
import com.bhavikm.calarm.app.features.signin.domain.repository.SignInRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.first

class SignInRepositoryImpl(
    private val authService: AuthService,
    private val appSettingsDao: AppSettingsDao,
) : SignInRepository {

    override suspend fun signIn(context: Activity): Result<FirebaseUser> =
        authService.signInWithGoogle(context).onSuccess {
            val userId = it.uid
            // Check if settings already exist for this user
            val defaultSettings = appSettingsDao.getSettings(userId).first()
            appSettingsDao.upsertSettings(defaultSettings)
        }

    override suspend fun getGoogleSignInIntent(activity: Activity): Intent {
        return authService.getGoogleSignInIntent(activity)
    }

    override fun processAuthCode(result: ActivityResult): Result<FirebaseUser> =
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)

            try {
                val account = task.getResult(ApiException::class.java)
                val authCode = account.serverAuthCode
                if (authCode != null) {
                    Log.d("AUTH_CODE", "Received: $authCode")
                    // 🔁 Send this authCode to your backend to exchange for access/refresh tokens
                } else {
                    Log.e("AUTH_CODE", "Auth code is null")
                }
                if (authService.currentUser != null) {
                    Result.success(authService.currentUser!!)
                } else {
                    Result.failure(Exception("No current user"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        } else {
            authService.signOut()
            val error =
                "Google Calendar scope permission denied by user or flow cancelled. Result code: ${result.resultCode}"
            Log.w(
                "HomeScreen",
                error,
            )
            Result.failure(Exception(error))
        }
}
