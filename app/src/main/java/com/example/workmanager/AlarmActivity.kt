package com.example.workmanager

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp


class AlarmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        turnScreenOnAndShowWhenLocked()

        setContent {
            AlarmScreen()
        }
    }


    @Suppress("DEPRECATION")
    private fun turnScreenOnAndShowWhenLocked() {
        // Always keep screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
        window.addFlags(WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        // Dismiss keyguard (Android 8.0+)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            keyguardManager.requestDismissKeyguard(
                this,
                object : KeyguardManager.KeyguardDismissCallback() {
                    override fun onDismissError() {
                        super.onDismissError()
                        println("onDismissError")
                    }

                    override fun onDismissCancelled() {
                        super.onDismissCancelled()
                        println("onDismissCancelled")
                    }

                    override fun onDismissSucceeded() {
                        super.onDismissSucceeded()
                        println("onDismissSucceeded")
                    }
                })
        }
        val keyguard = keyguardManager.newKeyguardLock("MyApp");
        keyguard.disableKeyguard();
//        }
    }
}

@Composable
fun AlarmScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Red),
        contentAlignment = Alignment.Center
    ) {
        Text("Event starting soon!", color = Color.White, fontSize = 24.sp)
    }
}
