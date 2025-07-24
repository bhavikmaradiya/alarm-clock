package com.sevenspan.calarm.app.features.signin.data.repository

import android.app.Activity
import android.app.PendingIntent
import android.util.Log
import androidx.activity.result.ActivityResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.messaging.FirebaseMessaging
import com.revenuecat.purchases.Purchases
import com.sevenspan.calarm.app.core.data.source.local.AppSettingsDao
import com.sevenspan.calarm.app.core.service.AuthService
import com.sevenspan.calarm.app.features.signin.domain.repository.SignInRepository
import kotlinx.coroutines.flow.first
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SignInRepositoryImpl(
    private val authService: AuthService,
    private val appSettingsDao: AppSettingsDao,
) : SignInRepository {
    override suspend fun signIn(context: Activity): Result<Unit> =
        authService.signInWithGoogle(context).fold(
            onSuccess = {
                Result.success(Unit)
            },
            onFailure = {
                Result.failure(it)
            },
        )

    override suspend fun getGoogleSignInIntent(activity: Activity): PendingIntent? {
        val shouldAskForCalendarScope = authService.isScopePermissionNeeded()
        if (!shouldAskForCalendarScope) {
            return null
        }
        return authService.getGoogleSignInIntent(activity)
    }

    override suspend fun subscribeToCalendarWebhook(authCode: String?): Result<FirebaseUser?> {
        val sessionId = authService.subscribeToCalendarWebhook(authCode).getOrNull()
        if (!sessionId.isNullOrBlank()) {
            val userId = authService.currentUser!!.uid
            val defaultSettings =
                appSettingsDao.getSettings(userId = userId)
                    .first()
            appSettingsDao.upsertSettings(
                settings = defaultSettings.copy(
                    sessionId = sessionId,
                ),
            )
            Purchases.sharedInstance.logIn(userId)
            val fcmToken = getFcmToken()
            if (!fcmToken.isNullOrBlank()) {
                authService.updateFcmToken(fcmToken)
            }
            return Result.success(authService.currentUser)
        }
        authService.signOut()
        Log.e("AUTH_CODE", "Failed to subscribe to calendar")
        return Result.failure(Exception("Failed to subscribe to calendar"))
    }

    override suspend fun processAuthCode(
        activity: Activity,
        result: ActivityResult,
    ): Result<FirebaseUser> = if (result.resultCode == Activity.RESULT_OK) {
        val authorizationResult =
            Identity.getAuthorizationClient(activity)
                .getAuthorizationResultFromIntent(result.data)
        try {
            val account = authorizationResult.toGoogleSignInAccount()
            val authCode = account?.serverAuthCode
            if (authCode != null) {
                Log.d("AUTH_CODE", "Received: $authCode")
                subscribeToCalendarWebhook(authCode)
            } else {
                authService.signOut()
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

    override suspend fun signOut() = authService.signOut()

    private suspend fun getFcmToken(): String? = suspendCoroutine { continuation ->
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                continuation.resume(task.result)
            } else {
                Log.w(
                    "FCM_TOKEN",
                    "Fetching FCM registration token failed in SignInRepo",
                    task.exception,
                )
                continuation.resume(null)
            }
        }
    }
}
