@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.workmanager.app.features.signin.presentation

import android.app.Activity
import android.util.Log
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.calendar.CalendarScopes
import org.koin.androidx.compose.koinViewModel

@Composable
fun SignInScreen(
    viewModel: SignInViewModel = koinViewModel(),
    onAuthSuccess: () -> Unit,
) {
    val activity = LocalActivity.current as Activity
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackBarHostState = remember { SnackbarHostState() }

    val googleCalendarScopeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
//            snackBarHostState.showSnackbar("Sign in successful for ${state.userData?.displayName}")
            onAuthSuccess.invoke()
        } else {
            Log.w(
                "HomeScreen",
                "Google Calendar scope permission denied by user or flow cancelled. Result code: ${result.resultCode}"
            )

        }
    }

    LaunchedEffect(state.status) {
        when (state.status) {
            SignInStatus.SUCCESS -> {
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .requestProfile()
                    .requestScopes(Scope(CalendarScopes.CALENDAR)) // <<< THIS IS WHERE YOU ASK FOR THE SCOPE
                    .build()
                val signInIntent = GoogleSignIn.getClient(activity, gso).signInIntent
                googleCalendarScopeLauncher.launch(signInIntent)
            }

            SignInStatus.ERROR   -> {
                snackBarHostState.showSnackbar("Sign in failed: ${state.error?.message}")
            }

            else                 -> {}
        }
    }



    SignInComposable(
        signInState = state,
        snackBarHostState = snackBarHostState,
        onSignInClick = { viewModel.signIn(activity) }
    )
}

@Composable
fun SignInComposable(
    signInState: SignInState = SignInState(),
    snackBarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onSignInClick: () -> Unit = {},
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackBarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Sign In") },
            )
        },

        ) { innerPadding ->
        Body(
            modifier = Modifier.padding(innerPadding),
            signInState = signInState,
            onSignInClick = onSignInClick
        )
    }
}

@Composable
fun Body(
    modifier: Modifier = Modifier,
    signInState: SignInState,
    onSignInClick: () -> Unit,
) {

    Box(
        modifier
            .fillMaxSize()
            .background(
                color = Color.LightGray.copy(alpha = 0.3f),
            ),
    ) {
        when (signInState.status) {
            SignInStatus.LOADING -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(alignment = Alignment.Center),
                    color = Color.Gray,
                )
            }

            SignInStatus.ERROR,
            SignInStatus.INITIAL,
                                 -> Button(
                modifier = Modifier.align(Alignment.Center),
                onClick = onSignInClick
            ) {
                Text("Sign in with Google")
            }

            SignInStatus.SUCCESS -> {
                Text(
                    modifier = Modifier.align(Alignment.Center),
                    text = "Welcome ${signInState.userData?.displayName}"
                )
            }
        }
    }
}

@Preview
@Composable
fun SignInScreenPreview() = SignInComposable()

