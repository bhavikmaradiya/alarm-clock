package com.bhavikm.calarm.app.core.service

import android.accounts.Account
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.bhavikm.calarm.BuildConfig
import com.bhavikm.calarm.app.core.data.source.local.AppSettingsDao
import com.bhavikm.calarm.app.data.network.ApiClient
import com.bhavikm.calarm.app.data.network.model.AuthCodeRequest
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.api.services.calendar.CalendarScopes
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.database
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

interface AuthService {
    val currentUser: FirebaseUser?
    val googleSignInUser: Account?

    val isUserSignedIn: Boolean

    suspend fun isUserSignedIn(): Boolean

    suspend fun signInWithGoogle(activity: Activity): Result<String>

    /*suspend fun getGoogleSignInIntent(activity: Activity): PendingIntent?*/
    suspend fun getGoogleSignInIntent(activity: Activity): Intent
    suspend fun signOut()

    suspend fun updateFcmToken(token: String? = null)
    suspend fun firebaseAuthWithGoogle(idToken: String): Result<FirebaseUser>
    suspend fun subscribeToCalendarWithAuthCode(authCode: String): Result<String>
}

class FirebaseAuthService(
    private val context: Context,
    private val settingsService: AppSettingsDao,
) : AuthService {
    private val auth: FirebaseAuth = Firebase.auth
    private var googleSignInClient: GoogleSignInClient? = null
    private val database: FirebaseDatabase = Firebase.database
    override val currentUser: FirebaseUser? get() = auth.currentUser
    override val googleSignInUser: Account?
        get() = GoogleSignIn.getLastSignedInAccount(context)?.account
    override val isUserSignedIn: Boolean
        get() = currentUser != null && googleSignInUser != null

    override suspend fun signInWithGoogle(activity: Activity): Result<String> =
        getCredentials(activity)

    override suspend fun signOut() {
        updateFcmToken()
        removeRefreshToken()
        if (currentUser != null) {
            auth.signOut()
        }
        googleSignInClient?.signOut()
        googleSignInClient = null
    }

    override suspend fun isUserSignedIn(): Boolean {
        return isUserSignedIn /*&& isRefreshTokenAvailable()*/
    }

    private suspend fun removeRefreshToken() {
        val user = currentUser ?: return
        val settings = settingsService.getSettings(user.uid).first()
        settingsService.upsertSettings(settings)
        /*try {
            database.reference
                .child("users")
                .child(user.uid)
                .child("refreshToken")
                .removeValue()
                .await()
        } catch (e: Exception) {
            Log.e("FirebaseAuthRepository", "removeRefreshToken: ", e)
        }*/
    }

    private suspend fun isRefreshTokenAvailable(): Boolean {
        val user = currentUser ?: return false
        val settings = settingsService.getSettings(user.uid).first()
        val localRefreshToken = settings.sessionId
        if (localRefreshToken.isNullOrEmpty()) return false

        val userData =
            try {
                database.reference
                    .child("users")
                    .child(user.uid)
                    .get()
                    .await()
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        if (userData?.exists() == true) {
            val refreshTokenChild = userData.child("refreshToken")
            if (!refreshTokenChild.exists()) {
                val refreshToken = refreshTokenChild
                    .value as String?
                if (refreshToken.isNullOrEmpty()) return false
                return localRefreshToken.compareTo(
                    refreshToken,
                    ignoreCase = false
                ) == 0
            }
        }
        return false
    }

    override suspend fun getGoogleSignInIntent(activity: Activity): Intent {
        if (googleSignInClient != null) return googleSignInClient!!.signInIntent
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .requestScopes(Scope(CalendarScopes.CALENDAR_READONLY))
            .requestServerAuthCode(
                BuildConfig.GOOGLE_SIGN_IN_SERVER_CLIENT_ID,
                true,
            )
            .build()
        googleSignInClient = GoogleSignIn.getClient(activity, gso)
        return googleSignInClient!!.signInIntent
    }

    override suspend fun updateFcmToken(token: String?) {
        val user = currentUser ?: return
        try {
            database.reference
                .child("users")
                .child(user.uid)
                .child("fcmToken")
                .setValue(token)
                .await()
        } catch (e: Exception) {
            Log.e("FirebaseAuthRepository", "updateFcmToken: ", e)
        }

    }

    /*override suspend fun getGoogleSignInIntent(activity: Activity): PendingIntent? =
        suspendCancellableCoroutine { continuation ->
            val requestedScopes = listOf(Scope(CalendarScopes.CALENDAR_READONLY))
            val authorizationRequest =
                AuthorizationRequest.builder()
                    .setRequestedScopes(requestedScopes)
                    .requestOfflineAccess(BuildConfig.GOOGLE_SIGN_IN_SERVER_CLIENT_ID, true)
                    .build()

            Identity.getAuthorizationClient(activity)
                .authorize(authorizationRequest)
                .addOnSuccessListener { result ->
                    continuation.resume(result.pendingIntent, null)
                    *//*  if (result.hasResolution()) {
                      } else {
                          continuation.resume(result.pendingIntent, null)
                      }*//*
                }
                .addOnFailureListener { e ->
                    continuation.resumeWithException(e)
                }
        }*/

    private suspend fun getCredentials(activity: Activity): Result<String> {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .setServerClientId(BuildConfig.GOOGLE_SIGN_IN_SERVER_CLIENT_ID)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val credentialManager = CredentialManager.create(activity)

        return try {
            val credentialResponse = credentialManager.getCredential(activity, request)
            handleSignIn(credentialResponse.credential)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun handleSignIn(credential: Credential): Result<String> =
        try {
            when (credential) {
                is GoogleIdTokenCredential -> {
                    val idToken = credential.idToken
                    Result.success(idToken)
//                    firebaseAuthWithGoogle(idToken)
                }

                is CustomCredential        -> {
                    if (credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        val googleIdTokenCredential =
                            GoogleIdTokenCredential.createFrom(credential.data)
                        Result.success(googleIdTokenCredential.idToken)
                        /*firebaseAuthWithGoogle(
                            googleIdTokenCredential.idToken
                        )*/
                    } else {
                        Result.failure(Exception("Not a Google ID Token. CustomCredential with type: ${credential.type}"))
                    }
                }

                else                       -> {
                    Result.failure(Exception("Not a Google ID Token. Credential type: ${credential::class.java.name}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun firebaseAuthWithGoogle(idToken: String): Result<FirebaseUser> =
        try {
            val authCredential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(authCredential).await()
            if (authResult.user != null) {
                Result.success(authResult.user!!)
            } else
                Result.failure(exception = Exception("Firebase user not found after successful authentication."))
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun subscribeToCalendarWithAuthCode(authCode: String): Result<String> {
        val userId =
            auth.currentUser?.uid ?: return Result.failure(Exception("User not authenticated"))
        try {
            val requestBody =
                AuthCodeRequest(authCode = authCode, userId = userId)
            val response = ApiClient.apiService
                .subscribeToCalendarChanges(
                    request = requestBody
                )

            val sessionId = response.body()?.sessionId
            Log.d("Calendar Subscription", "Session ID: $sessionId")

            if (response.isSuccessful && !sessionId.isNullOrBlank()) {
                return Result.success(value = sessionId)
            }
            return Result.failure(exception = Exception(response.message()))
        } catch (e: Exception) {
            println("Exception during API call: ${e.message}")
            return Result.failure(e)
        }
    }
}
