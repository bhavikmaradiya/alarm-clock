package com.sevenspan.calarm.app.features.signin.domain.repository

import android.app.Activity
import android.app.PendingIntent
import androidx.activity.result.ActivityResult
import com.google.firebase.auth.FirebaseUser

interface SignInRepository {
    suspend fun signIn(context: Activity): Result<Unit>

    suspend fun getGoogleSignInIntent(activity: Activity): PendingIntent?

    suspend fun processAuthCode(activity: Activity, result: ActivityResult): Result<FirebaseUser>
    suspend fun signOut()

    suspend fun subscribeToCalendarWebhook(authCode: String? = null): Result<FirebaseUser?>
}
