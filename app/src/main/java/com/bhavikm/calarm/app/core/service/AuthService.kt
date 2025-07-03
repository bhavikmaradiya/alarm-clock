package com.bhavikm.calarm.app.core.service

import android.accounts.Account
import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.bhavikm.calarm.BuildConfig
import com.google.android.gms.auth.api.signin.GoogleSignIn
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
import kotlinx.coroutines.tasks.await

interface AuthService {
    val currentUser: FirebaseUser?
    val googleSignInUser: Account?

    val isUserSignedIn: Boolean

    suspend fun signInWithGoogle(context: Activity): Result<FirebaseUser>

    /*suspend fun getGoogleSignInIntent(activity: Activity): PendingIntent?*/
    suspend fun getGoogleSignInIntent(activity: Activity): Intent
    fun signOut()
}

class FirebaseAuthService(private val context: Context) : AuthService {
    private val auth: FirebaseAuth = Firebase.auth
    override val currentUser: FirebaseUser? get() = auth.currentUser
    override val googleSignInUser: Account?
        get() = GoogleSignIn.getLastSignedInAccount(context)?.account
    override val isUserSignedIn: Boolean
        get() = currentUser != null && googleSignInUser != null

    override suspend fun signInWithGoogle(context: Activity): Result<FirebaseUser> =
        getCredentials(context)

    override fun signOut() {
        if (currentUser != null) {
            auth.signOut()
        }
    }

    override suspend fun getGoogleSignInIntent(activity: Activity): Intent {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .requestScopes(Scope(CalendarScopes.CALENDAR_READONLY))
            .requestServerAuthCode(
                BuildConfig.GOOGLE_SIGN_IN_SERVER_CLIENT_ID,
                true,
            )
            .build()
        return GoogleSignIn.getClient(activity, gso).signInIntent
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

    private suspend fun getCredentials(context: Activity): Result<FirebaseUser> {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .setServerClientId(BuildConfig.GOOGLE_SIGN_IN_SERVER_CLIENT_ID)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val credentialManager = CredentialManager.create(context)

        return try {
            val credentialResponse = credentialManager.getCredential(context, request)
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
                    firebaseAuthWithGoogle(idToken)
                }

                is CustomCredential        -> {
                    if (credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        val googleIdTokenCredential =
                            GoogleIdTokenCredential.createFrom(credential.data)
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
}
