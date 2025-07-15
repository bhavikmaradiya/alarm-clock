package com.sevenspan.calarm.app.core.service

import android.app.Activity
import android.app.PendingIntent
import android.util.Log
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.database
import com.sevenspan.calarm.BuildConfig
import com.sevenspan.calarm.app.core.data.source.local.AppSettingsDao
import com.sevenspan.calarm.app.data.network.ApiClient
import com.sevenspan.calarm.app.data.network.model.AuthCodeRequest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await

interface AuthService {
    val currentUser: FirebaseUser?

    val isUserSignedIn: Boolean

    suspend fun isUserSignedIn(): Boolean

    suspend fun signInWithGoogle(activity: Activity): Result<FirebaseUser>

    suspend fun getGoogleSignInIntent(activity: Activity): PendingIntent?
    suspend fun signOut()

    suspend fun updateFcmToken(token: String? = null)

    suspend fun isPermissionNeeded(): Boolean
    suspend fun subscribeToCalendarWebhook(authCode: String? = null): Result<String>
    suspend fun fetchBaseUrlFromFirebase(): String?
}

class FirebaseAuthService(
    private val settingsService: AppSettingsDao,
) : AuthService {
    private val auth: FirebaseAuth = Firebase.auth
    private val database: FirebaseDatabase = Firebase.database
    override val currentUser: FirebaseUser? get() = auth.currentUser
    override val isUserSignedIn: Boolean
        get() = currentUser != null

    override suspend fun signInWithGoogle(activity: Activity): Result<FirebaseUser> =
        getCredentials(activity)

    override suspend fun signOut() {
        updateFcmToken()
        removeRefreshToken()
        if (currentUser != null) {
            auth.signOut()
        }
        /*googleSignInClient?.signOut()
        googleSignInClient = null*/
    }

    override suspend fun isUserSignedIn(): Boolean {
        return isUserSignedIn && isRefreshTokenAvailable()
    }

    private suspend fun removeRefreshToken() {
        val user = currentUser ?: return
        val settings = settingsService.getSettings(user.uid).first()
        settingsService.upsertSettings(settings.copy(sessionId = null))
    }

    private suspend fun isRefreshTokenAvailable(): Boolean {
        val user = currentUser ?: return false
        val settings = settingsService.getSettings(user.uid).first()
        val localRefreshToken = settings.sessionId
        if (localRefreshToken.isNullOrEmpty()) {
            Log.d("FirebaseAuthRepository", "Room refresh token is null")
            return false
        }

        val userData =
            try {
                database.reference
                    .child("users")
                    .child(user.uid)
                    .get()
                    .await()
            } catch (e: Exception) {
                Log.e("FirebaseAuthRepository", "isRefreshTokenAvailable: ", e)
                e.printStackTrace()
                null
            }
        if (userData?.exists() == true) {
            val refreshTokenChild = userData.child("refreshToken")
            if (refreshTokenChild.exists()) {
                val refreshToken = refreshTokenChild
                    .value as String?
                if (refreshToken.isNullOrEmpty()) {
                    Log.d(
                        "FirebaseAuthRepository",
                        "Firebase refresh token is null"
                    )
                    return false
                }
                return localRefreshToken.compareTo(
                    refreshToken,
                    ignoreCase = false
                ) == 0
            }
        }
        Log.d("FirebaseAuthRepository", "Firebase refresh token is null")
        return false
    }

    override suspend fun updateFcmToken(token: String?) {
        val user = currentUser ?: return
        Log.d("FirebaseAuthRepository", "updateFcmToken: $token for ${user.email}")
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

    override suspend fun getGoogleSignInIntent(
        activity: Activity,
    ): PendingIntent? {
        val requestedScopes = listOf(
            Scope("openid"),
            Scope("https://www.googleapis.com/auth/userinfo.email"),
            Scope("https://www.googleapis.com/auth/userinfo.profile"),
            Scope("https://www.googleapis.com/auth/calendar.readonly")
        )

        val authorizationRequest = AuthorizationRequest.builder()
            .setRequestedScopes(requestedScopes)
            .requestOfflineAccess(BuildConfig.GOOGLE_SIGN_IN_SERVER_CLIENT_ID, true)
            .build()

        return suspendCancellableCoroutine { continuation ->
            Identity.getAuthorizationClient(activity)
                .authorize(authorizationRequest)
                .addOnSuccessListener { result ->
                    if (result.hasResolution()) {
                        continuation.resume(result.pendingIntent) { cause, _, _ -> null }
                    } else {
                        continuation.resume(null) { cause, _, _ -> null }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("GoogleSignIn", "Failed to get Google Sign-In intent", e)
                    continuation.resume(null) { cause, _, _ -> null }
                }
        }
    }

    override suspend fun isPermissionNeeded(): Boolean {
        val userId = currentUser?.uid ?: return true
        val response = ApiClient.apiService
            .shouldShowAuthScreen(userId = userId)

        return response.body()?.needsScopeConsent ?: return true
    }

    private suspend fun getCredentials(activity: Activity): Result<FirebaseUser> {
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
            val credentialResponse = credentialManager.getCredential(
                context = activity,
                request = request
            )
            handleSignIn(credentialResponse.credential)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun handleSignIn(credential: Credential): Result<FirebaseUser> =
        try {
            when (credential) {
                is GoogleIdTokenCredential -> {
                    val idToken = credential.idToken
                    Result.success(idToken)
                    firebaseAuthWithGoogle(idToken)
                }

                is CustomCredential        -> {
                    if (credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        val googleIdTokenCredential =
                            GoogleIdTokenCredential.createFrom(credential.data)
                        Result.success(googleIdTokenCredential.idToken)
                        firebaseAuthWithGoogle(
                            googleIdTokenCredential.idToken
                        )
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

    private suspend fun firebaseAuthWithGoogle(idToken: String): Result<FirebaseUser> =
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

    override suspend fun subscribeToCalendarWebhook(authCode: String?): Result<String> {
        val userId =
            auth.currentUser?.uid
        if (userId == null) {
            return Result.failure(Exception("User not authenticated"))
        }
        try {
            val requestBody =
                AuthCodeRequest(
                    authCode = authCode,
                    userId = userId
                )
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

    override suspend fun fetchBaseUrlFromFirebase(): String? {
        return try {
            val snapshot = database.getReference("config/baseUrl")
                .get().await()
            snapshot.getValue(String::class.java)
        } catch (e: Exception) {
            Log.e("AppInit", "Failed to fetch base URL", e)
            null
        }
    }
}
