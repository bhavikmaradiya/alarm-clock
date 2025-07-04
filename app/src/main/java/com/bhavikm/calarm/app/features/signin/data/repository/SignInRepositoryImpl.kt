package com.bhavikm.calarm.app.features.signin.data.repository

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResult
import com.bhavikm.calarm.app.core.data.source.local.AppSettingsDao
import com.bhavikm.calarm.app.core.service.AuthService
import com.bhavikm.calarm.app.data.network.ApiClient
import com.bhavikm.calarm.app.data.network.model.AuthCodeRequest
import com.bhavikm.calarm.app.features.signin.domain.repository.SignInRepository
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.first
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

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

    /*override suspend fun getGoogleSignInIntent(activity: Activity): PendingIntent? {
        return authService.getGoogleSignInIntent(activity)
    }*/

    override suspend fun processAuthCode(result: ActivityResult): Result<FirebaseUser> =
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)

            try {
                val account = task.getResult(ApiException::class.java)
                val authCode = account.serverAuthCode
                if (authCode != null) {
//                    val userId = authService.currentUser?.uid.orEmpty()
//                    Log.d("AUTH_CODE", "Received: $authCode")
//                    val fcmToken = getFcmToken()
//                    if (fcmToken != null) {
//                        authService.updateFcmToken(fcmToken)
//                    }
//                    // 🔁 Subscribe to calendar events
//                    subscribeToCalendarWithAuthCode(authCode, userId)
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

    override suspend fun processAuthCode(
        activity: Activity,
        result: ActivityResult,
    ): Result<FirebaseUser> =

        if (result.resultCode == Activity.RESULT_OK) {
            val authorizationResult =
                Identity.getAuthorizationClient(activity)
                    .getAuthorizationResultFromIntent(result.data)
            try {
                val account = authorizationResult.toGoogleSignInAccount()
                val authCode = account?.serverAuthCode
                if (authCode != null) {
                    Log.d("AUTH_CODE", "Received: $authCode")
                    // 🔁 Send this authCode to your backend to exchange for access/refresh tokens
                    /*subscribeToCalendarWithAuthCode(
                        authCode,
                        authService.currentUser?.uid.orEmpty()
                    )*/
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

    private suspend fun subscribeToCalendarWithAuthCode(authCode: String, userId: String) {
        try {
            val requestBody =
                AuthCodeRequest(authCode = authCode, userId = userId)
            val response = ApiClient.apiService.subscribeToCalendarChanges(
                requestBody
            )

            println("Response: $response")
        } catch (e: Exception) {
            println("Exception during API call: ${e.message}")
        }
    }


    private suspend fun getFcmToken(): String? = suspendCoroutine { continuation ->
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                continuation.resume(task.result)
            } else {
                Log.w(
                    "FCM_TOKEN",
                    "Fetching FCM registration token failed in SignInRepo",
                    task.exception
                )
                continuation.resume(null) // Or continuation.resumeWithException(task.exception!!)
            }
        }
    }
}
