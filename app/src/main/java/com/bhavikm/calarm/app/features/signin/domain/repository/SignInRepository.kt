package com.bhavikm.calarm.app.features.signin.domain.repository

import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResult
import com.google.firebase.auth.FirebaseUser

interface SignInRepository {
    suspend fun signIn(context: Activity): Result<FirebaseUser>

    /*suspend fun getGoogleSignInIntent(activity: Activity): PendingIntent?*/
    suspend fun getGoogleSignInIntent(activity: Activity): Intent
    suspend fun processAuthCode(result: ActivityResult): Result<FirebaseUser>
    suspend fun processAuthCode(activity: Activity, result: ActivityResult): Result<FirebaseUser>
}
