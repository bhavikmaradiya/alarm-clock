package com.bhavikm.calarm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import com.bhavikm.calarm.navigation.Navigation
import com.bhavikm.calarm.ui.theme.WorkManagerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EventApp()
        }
    }
}

@Composable
private fun EventApp() {
    WorkManagerTheme {
        Navigation()
    }
}
