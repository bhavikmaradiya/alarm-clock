package com.example.workmanager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class OverlayWorker(
    private val context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        setForegroundAsync(createForegroundInfo())

        if (Settings.canDrawOverlays(context)) {
            withContext(Dispatchers.Main) { showOverlay() }
        } else {
            // You could notify user somehow here
        }

        delay(2 * 60 * 1000) // Keep overlay visible for 2 minutes
        removeOverlay()
        return Result.success()
    }

    private fun createForegroundInfo(): ForegroundInfo {
        val channelId = "overlay_worker_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(
                channelId,
                "Overlay Worker",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(chan)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("Pre-alarm running")
            .setContentText("Foreground worker active")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        return ForegroundInfo(12345, notification)
    }

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null

    private fun showOverlay() {
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val inflater = LayoutInflater.from(context)
        overlayView = inflater.inflate(R.layout.overlay_layout, null)

        val composeView = overlayView!!.findViewById<ComposeView>(R.id.compose_overlay)
        composeView.setViewTreeLifecycleOwner(lifecycleOwner = object : LifecycleOwner {
            override val lifecycle: Lifecycle
                get() {
                    return LifecycleRegistry(this)
                }
        })
        composeView.setContent {
            MaterialTheme {
                OverlayContent {
                    removeOverlay()
                }
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.CENTER
        windowManager.addView(overlayView, params)
    }

    private fun removeOverlay() {
        overlayView?.let {
            windowManager.removeView(it)
            overlayView = null
        }
    }
}
