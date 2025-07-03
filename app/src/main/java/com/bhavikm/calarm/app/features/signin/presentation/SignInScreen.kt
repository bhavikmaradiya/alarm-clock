package com.bhavikm.calarm.app.features.signin.presentation

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bhavikm.calarm.CalarmApp.Companion.isNetworkAvailable
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel


@Composable
fun SignInScreen(
    viewModel: SignInViewModel = koinViewModel(),
    onAuthSuccess: () -> Unit,
) {
    val activity = LocalActivity.current as Activity
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackBarHostState = remember { SnackbarHostState() }
    val uiEvent = viewModel.uiEvent
    val scope = rememberCoroutineScope()

    val channel = NotificationChannel(
        "calendar_updates",
        "Calendar Updates",
        NotificationManager.IMPORTANCE_HIGH,
    ).apply {
        description = "Notifications for calendar event updates"
    }

    val notificationManager =
        activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.createNotificationChannel(channel)

    val googleCalendarScopeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        viewModel.processResult(result)
    }


    LaunchedEffect(Unit) {
        uiEvent.collect {
            when (val event = it) {
                is SignInEvent.OnSignInIntentGenerated -> {
                    googleCalendarScopeLauncher.launch(event.intent)
                }

                else                                   -> {}
            }
        }
    }

    LaunchedEffect(state.status) {
        when (state.status) {
            SignInStatus.SUCCESS -> {
                onAuthSuccess.invoke()
            }

            SignInStatus.ERROR   -> {
                snackBarHostState.showSnackbar("Sign in failed: ${state.error}")
            }

            else                 -> {}
        }
    }

    SignInComposable(
        signInState = state,
        snackBarHostState = snackBarHostState,
        onSignInClick = {
            if (isNetworkAvailable(activity)) {
                viewModel.signIn(activity)
            } else {
                scope.launch {
                    snackBarHostState.showSnackbar("Please check your internet connection")
                }
            }
        },
    )
}

@Composable
fun SignInComposable(
    signInState: SignInState,
    snackBarHostState: SnackbarHostState,
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
            onSignInClick = onSignInClick,
        )
    }
}

@Composable
fun Body(
    signInState: SignInState,
    onSignInClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .fillMaxSize()

    ) {
        when (signInState.status) {
            SignInStatus.LOADING -> {
                LoadingIndicator(
                    modifier = Modifier.align(alignment = Alignment.Center),
                    color = Color.Gray,
                )
            }

            SignInStatus.ERROR,
            SignInStatus.INITIAL,
                                 -> Button(
                modifier = Modifier.align(Alignment.Center),
                onClick = onSignInClick,
            ) {
                Text("Sign in with Google")
            }

            SignInStatus.SUCCESS -> {
                Text(
                    modifier = Modifier.align(Alignment.Center),
                    text = "Welcome ${signInState.userData?.displayName}",
                )
            }
        }
    }
}

