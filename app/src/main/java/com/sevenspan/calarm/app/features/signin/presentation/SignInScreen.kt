package com.sevenspan.calarm.app.features.signin.presentation

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sevenspan.calarm.CalarmApp.Companion.isNetworkAvailable
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun PermissionsInfoDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            modifier = Modifier.padding(vertical = LocalConfiguration.current.screenHeightDp.dp * 0.12f),
            title = {
                Text("How Calarm Works With Your Google Account")
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        "To bring you smart, calendar-aware alarms, Calarm needs to connect with your Google Account. Here's a clear breakdown of what we access and why it's important for the app's features:",
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "1. Google Calendar (Read-Only Access)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "   • What we access: Your calendar events, including their names, dates, times, and attendees.",
                        fontSize = 13.sp
                    )
                    Text(
                        "   • Why it helps you: This allows Calarm to display your upcoming schedule right in the app. Most importantly, you can then easily pick any event and set an alarm for it. No more manually creating reminders for every meeting or appointment!",
                        fontSize = 13.sp
                    )
                    Text(
                        "   • Good to know: Calarm will only read your event details. We will never modify, delete, or add events to your calendar.",
                        fontSize = 13.sp,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        "2. Basic Profile Info (Email & Name)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "   • What we access: Your basic Google profile information, such as your name and email address.",
                        fontSize = 13.sp
                    )
                    Text(
                        "   • Why it helps you: This is used to securely sign you into Calarm and make sure your calendar data is correctly linked to your account. It also helps us personalize your experience within the app, for instance, by showing your account details.",
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        "3. Offline Access (for Calendar Sync & Updates)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "   • What we access: The ability to refresh your calendar data even when you're not actively using Calarm.",
                        fontSize = 13.sp
                    )
                    Text(
                        "   • Why it helps you: This ensures Calarm can keep your event information accurate and up-to-date in the background. So, if an event is rescheduled or added to your calendar, Calarm knows about it and your alarms will always be timely. You don't have to manually sync every time!",
                        fontSize = 13.sp
                    )
                    Text(
                        "   • Good to know: We use a secure token (not your password!) provided by Google to do this. This means Calarm can sync efficiently without you needing to sign in repeatedly.",
                        fontSize = 13.sp,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "Your Privacy is Our Priority",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "We are committed to protecting your personal information. Calarm uses these permissions only to provide its core features and improve your experience. We do not sell your data or share your personal calendar details with any third-party advertisers. You have full control and can review or revoke these permissions at any time through your Google Account settings.",
                        fontSize = 14.sp,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("Got it!")
                }
            }
        )
    }
}

@Composable
fun SignInScreen(
    viewModel: SignInViewModel = koinViewModel(),
    onAuthSuccess: () -> Unit,
) {
    val activity = LocalActivity.current as Activity
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackBarHostState = remember { SnackbarHostState() }
    val uiEvent = viewModel.uiEvent
    val coroutineScope = rememberCoroutineScope()

    var showPermissionsInfoDialog by rememberSaveable { mutableStateOf(true) }

    val googleCalendarScopeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { result -> viewModel.processResult(activity, result) }

    LaunchedEffect(Unit) {
        uiEvent.collect {
            when (val event = it) {
                is SignInEvent.OnSignInIntentGenerated -> {
                    if (event.intent != null) {
                        googleCalendarScopeLauncher.launch(
                            input = IntentSenderRequest.Builder(
                                intentSender = event.intent.intentSender,
                            ).build(),
                        )
                    }
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


    PermissionsInfoDialog(
        showDialog = showPermissionsInfoDialog,
        onDismiss = {
            showPermissionsInfoDialog = false
        }
    )

    SignInComposable(
        signInState = state,
        snackBarHostState = snackBarHostState,
        onSignInClick = {
            if (showPermissionsInfoDialog) {
                showPermissionsInfoDialog = false
            }
            coroutineScope.launch {
                if (isNetworkAvailable(activity)) {
                    viewModel.signIn(activity)
                } else {
                    coroutineScope.launch {
                        snackBarHostState.showSnackbar("Please check your internet connection")
                    }
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
            .fillMaxSize(),
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
