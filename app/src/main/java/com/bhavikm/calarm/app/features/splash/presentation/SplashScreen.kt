package com.bhavikm.calarm.app.features.splash.presentation

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import org.koin.androidx.compose.koinViewModel

@Composable
fun SplashScreen(
    viewModel: SplashViewModel = koinViewModel(),
    onAuthCheck: (isAuthenticated: Boolean) -> Unit,
) {
    val currentOnAuthCheck by rememberUpdatedState(onAuthCheck)
    val event = viewModel.uiEvent

    createCalendarNotificationChannel(context = LocalContext.current)
    LaunchedEffect(Unit) {
        event.collect {
            when (it) {
                is SplashEvent.NavigateToHome   -> {
                    currentOnAuthCheck(true)
                }

                is SplashEvent.NavigateToSignIn -> {
                    currentOnAuthCheck(false)
                }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text("Loading...")
    }
}

fun createCalendarNotificationChannel(context: Context) {
    val channelId = "calendar_updates"
    val name = "Calendar Updates"
    val descriptionText = "Notifies you about your calendar events and changes"
    val importance = NotificationManager.IMPORTANCE_HIGH

    val channel = NotificationChannel(channelId, name, importance).apply {
        description = descriptionText
        enableLights(true)
        enableVibration(true)
        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
    }

    val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.createNotificationChannel(channel)
}
