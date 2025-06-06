package com.example.workmanager.app.features.splash.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.auth.FirebaseUser
import org.koin.compose.koinInject

@Composable
fun SplashScreen(currentUser: FirebaseUser?, onAuthChecked: (isAuthenticated: Boolean) -> Unit) {
    val context = LocalContext.current
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Loading...")
    }

    LaunchedEffect(Unit) {
        onAuthChecked(currentUser != null && GoogleSignIn.getLastSignedInAccount(context)?.account != null)
    }
}