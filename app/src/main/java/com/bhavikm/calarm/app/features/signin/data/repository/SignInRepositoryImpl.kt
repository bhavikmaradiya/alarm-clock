package com.bhavikm.calarm.app.features.signin.data.repository

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResult
import com.bhavikm.calarm.app.core.data.source.local.AppSettingsDao
import com.bhavikm.calarm.app.core.service.AuthService
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
    private var idToken: String? = null
    override suspend fun signIn(context: Activity): Result<Unit> {
        return authService.signInWithGoogle(context).fold(
            onSuccess = {
                idToken = it
                Result.success(Unit)
            },
            onFailure = {
                Result.failure(it)
            }
        )
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
                if (idToken != null && authCode != null) {
                    // 🔁 Subscribe to calendar events and save refrshToken for identifying unique session
//                    authService.subscribeToCalendarWithAuthCode(authCode)
//                        .onSuccess { sessionId ->
                    authService.firebaseAuthWithGoogle(idToken = idToken!!)
                        .onSuccess {
                            val defaultSettings =
                                appSettingsDao.getSettings(userId = it.uid)
                                    .first()
                            appSettingsDao.upsertSettings(
                                settings = defaultSettings.copy(
//                                            sessionId = sessionId
                                )
                            )
                            val fcmToken = getFcmToken()
                            if (fcmToken != null) {
                                authService.updateFcmToken(fcmToken)
                            }
                        }
//                        }
                } else {
                    Log.e("AUTH_CODE", "Auth code or idToken is null")
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
            val error =
                "Google Calendar scope permission denied by user or flow cancelled. Result code: ${result.resultCode}"
            Log.w(
                "HomeScreen",
                error,
            )
            Result.failure(Exception(error))
        }


    override suspend fun signOut() = authService.signOut()


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
