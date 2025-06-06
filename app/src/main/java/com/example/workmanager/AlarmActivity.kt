package com.example.workmanager

import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.workmanager.app.core.domain.model.CalendarEvent
import com.example.workmanager.app.core.domain.model.CalendarEventBundleConverter.toCalendarEvent
import com.example.workmanager.app.core.domain.model.EventStatus
import com.meticha.triggerx.TriggerXActivity
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Simple Date Formatters
private val timeFormatter = SimpleDateFormat("h:mm a", Locale.getDefault())
private val dateFormatter = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
private const val TAG = "AlarmActivity"

class AlarmActivity : TriggerXActivity() {

    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        playNotificationSound()
    }

    private fun playNotificationSound() {
        try {
            val notificationSoundUri: Uri? = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            if (notificationSoundUri != null) {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(this@AlarmActivity, notificationSoundUri)
                    setOnPreparedListener { start() }
                    isLooping = true
                    setOnCompletionListener { mp ->
                        mp.reset()
                        mp.release()
                        mediaPlayer = null
                    }
                    setOnErrorListener { mp, what, extra ->
                        Log.e(TAG, "MediaPlayer error: what: $what, extra: $extra")
                        mp.reset()
                        mp.release()
                        mediaPlayer = null
                        true // True if the method handled the error, false if it didn't.
                    }
                    prepareAsync() // Prepare asynchronously to avoid blocking the main thread
                }
            } else {
                Log.w(TAG, "Default notification sound URI is null.")
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error setting up MediaPlayer for notification sound", e)
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: IllegalStateException) {
            Log.e(TAG, "IllegalStateException for MediaPlayer", e)
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }


    @Composable
    override fun AlarmContent() {
        val bundle = remember { intent?.getBundleExtra("ALARM_DATA") }
        val calendarEvent: CalendarEvent? = remember(bundle) {
            bundle?.toCalendarEvent()
        }

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = Color.White, // Consider using MaterialTheme.colorScheme.surface
                        shape = RoundedCornerShape(32.dp)
                    )
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Trigger Icon",
                    tint = Color(0xFF111111), // Consider MaterialTheme.colorScheme.onSurface
                    modifier = Modifier.size(80.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (calendarEvent != null) {
                    Text(
                        text = calendarEvent.eventName,
                        fontSize = 36.sp, // Slightly reduced for potentially longer names
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111111), // Consider MaterialTheme.colorScheme.onSurface
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Time: ${timeFormatter.format(Date(calendarEvent.startTimeMillis))} - ${
                            timeFormatter.format(
                                Date(calendarEvent.endTimeMillis)
                            )
                        }",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF333333), // Consider MaterialTheme.colorScheme.onSurfaceVariant
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Date: ${dateFormatter.format(Date(calendarEvent.startTimeMillis))}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF333333),
                        textAlign = TextAlign.Center
                    )

                    calendarEvent.location?.takeIf { it.isNotBlank() }?.let {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Location: $it",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF555555), // Consider MaterialTheme.colorScheme.onSurfaceVariant
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Status: ${calendarEvent.eventStatus.name}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = getStatusColor(calendarEvent.eventStatus), // Using a helper for status color
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(50.dp))
                    Button( onClick = {
                        mediaPlayer?.stop()
                        finish()
                    }) {
                        Text(text = "STOP")
                    }
                } else {
                    Text(
                        text = "Event Data Unavailable",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111111),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Could not load event details.",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF333333),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
    }
}

// Helper function to get a color based on EventStatus
// This should ideally be consistent with what's in your HomeScreen or a shared utility
@Composable
private fun getStatusColor(status: EventStatus): Color {
    return when (status) {
        EventStatus.PENDING   -> Color(0xFFFFA726) // Orange500
        EventStatus.SCHEDULED -> Color(0xFF66BB6A) // Green400
        EventStatus.COMPLETED -> Color(0xFF808080) // Grey
        EventStatus.CANCELLED -> Color(0xFFEF5350) // Red400
    }
}
