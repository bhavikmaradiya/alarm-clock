package com.example.workmanager.app.features.signin.data.repository

import android.app.Activity
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import arrow.core.Either
import arrow.core.Either.Companion.catch
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import com.example.workmanager.app.core.data.source.local.AppSettingsDao
import com.example.workmanager.app.core.domain.model.AppSettings
import com.example.workmanager.app.features.signin.domain.repository.SignInRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.auth
import kotlinx.coroutines.tasks.await

data class SignInError(
    val message: String,
    val cause: Throwable? = null, // Optional: to hold the original exception
)

class SignInRepositoryImpl(val appSettingsDao: AppSettingsDao) : SignInRepository {
    private val auth: FirebaseAuth = Firebase.auth

    override suspend fun getCredentials(context: Activity): Either<SignInError, FirebaseUser> {
        if (auth.currentUser != null) {
            auth.signOut()
        }
        val account = GoogleSignIn.getLastSignedInAccount(context)?.account
        if (account != null) {
        }
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .setServerClientId("66527400428-910mm6r3b06guhcr694kst3h0pkpv4si.apps.googleusercontent.com")
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
        val credentialManager = CredentialManager.create(context)
        val operation: Either<Throwable, GetCredentialResponse> = catch {
            credentialManager.getCredential(context, request)
        }
        return operation.mapLeft { credentialError ->
            SignInError(
                "Credential Manager operation failed: ${credentialError.message}",
                credentialError
            )
        }.flatMap { credentialResponse ->
            handleSignIn(credentialResponse.credential)
        }
    }

    private suspend fun handleSignIn(credential: Credential): Either<SignInError, FirebaseUser> {
        return when (credential) {
            is GoogleIdTokenCredential -> {
                catch { credential.idToken }
                    .mapLeft { e ->
                        SignInError(
                            "Failed to process Google ID Token from GoogleIdTokenCredential: ${e.message}",
                            e
                        )
                    }
                    .flatMap { idToken -> firebaseAuthWithGoogle(idToken) }
            }

            is CustomCredential        -> {
                if (credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    catch { GoogleIdTokenCredential.createFrom(credential.data) }
                        .mapLeft { e -> // Catches GoogleIdTokenParsingException etc.
                            SignInError(
                                "Failed to create Google ID Token from CustomCredential: ${e.message}",
                                e
                            )
                        }
                        .flatMap { googleIdTokenCredential ->
                            firebaseAuthWithGoogle(
                                googleIdTokenCredential.idToken
                            )
                        }
                } else {
                    SignInError("Not a Google ID Token. CustomCredential with type: ${credential.type}").left()
                }
            }

            else                       -> {
                SignInError("Not a Google ID Token. Credential type: ${credential::class.java.name}").left()
            }
        }
    }

    private suspend fun firebaseAuthWithGoogle(idToken: String): Either<SignInError, FirebaseUser> {
        return catch {
            GoogleAuthProvider.getCredential(idToken, null)
        }.mapLeft { e ->
            SignInError("Failed to get Google Auth Credential: ${e.message}", e)
        }.flatMap { authCredential ->
            catch { auth.signInWithCredential(authCredential).await() }
                .mapLeft { e -> SignInError("Firebase authentication failed: ${e.message}", e) }
                .flatMap { authResult ->
                    authResult.user?.let { firebaseUser ->
                        val userId = firebaseUser.uid
                        // Check if settings already exist for this user
                        val existingSettings = appSettingsDao.getSettingsOnce(userId)
                        if (existingSettings == null) {
                            // No settings found, create and save default AppSettings
                            val defaultSettings = AppSettings(userId = userId)
                            appSettingsDao.upsertSettings(defaultSettings)
                        }
                        firebaseUser.right() // Return the user
                    } ?: SignInError("Firebase user not found after successful authentication.").left()
                }
        }
    }
}