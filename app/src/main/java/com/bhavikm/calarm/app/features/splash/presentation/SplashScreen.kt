package com.bhavikm.calarm.app.features.splash.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.koin.androidx.compose.koinViewModel

@Composable
fun SplashScreen(
    viewModel: SplashViewModel = koinViewModel(),
    onAuthCheck: (isAuthenticated: Boolean) -> Unit,
) {
    val currentOnAuthCheck by rememberUpdatedState(onAuthCheck)
    val event = viewModel.uiEvent
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
