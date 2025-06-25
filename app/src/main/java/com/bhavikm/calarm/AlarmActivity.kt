package com.bhavikm.calarm

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.bhavikm.calarm.app.core.model.CalendarEvent
import com.bhavikm.calarm.app.core.model.CalendarEventBundleConverter.toCalendarEvent
import com.bhavikm.calarm.app.core.model.EventStatus
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import com.google.firebase.auth.auth
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
    private var originalVolume: Int? = null
    private var audioManager: AudioManager? = null

    val firebaseAuthUser = Firebase.auth.currentUser

    private var analytics: FirebaseAnalytics = Firebase.analytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bundle = intent?.getBundleExtra("ALARM_DATA")
        if (bundle == null || bundle.isEmpty || bundle.toCalendarEvent() == null) {
            analytics.logEvent(TAG) {
                param("User", firebaseAuthUser?.email ?: "Unknown")
                param("error", "ALARM_DATA is null or empty")
            }
        }
        playNotificationSound()
    }

    private fun playNotificationSound() {
        try {
            audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            originalVolume = audioManager?.getStreamVolume(AudioManager.STREAM_RING)
            val maxVolume = audioManager?.getStreamMaxVolume(AudioManager.STREAM_RING)
            maxVolume?.let {
                audioManager?.setStreamVolume(AudioManager.STREAM_RING, it, 0)
            }

            val notificationSoundUri: Uri? =
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            if (notificationSoundUri != null) {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(this@AlarmActivity, notificationSoundUri)
                    setOnPreparedListener { start() }
                    isLooping = true
                    setOnCompletionListener { mp ->
                        mp.reset()
                        mp.release()
                        mediaPlayer = null
                        // Restore original volume
                        originalVolume?.let {
                            audioManager?.setStreamVolume(
                                AudioManager.STREAM_RING,
                                it,
                                0,
                            )
                        }
                    }
                    setOnErrorListener { mp, what, extra ->
                        Log.e(TAG, "MediaPlayer error: what: $what, extra: $extra")
                        mp.reset()
                        mp.release()
                        mediaPlayer = null
                        // Restore original volume
                        originalVolume?.let {
                            audioManager?.setStreamVolume(
                                AudioManager.STREAM_RING,
                                it,
                                0,
                            )
                        }
                        true // True if the method handled the error, false if it didn't.
                    }
                    prepareAsync() // Prepare asynchronously to avoid blocking the main thread
                }
            } else {
                Log.w(TAG, "Default notification sound URI is null.")
                // Restore original volume if sound URI is null
                originalVolume?.let {
                    audioManager?.setStreamVolume(
                        AudioManager.STREAM_RING,
                        it,
                        0,
                    )
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error setting up MediaPlayer for notification sound", e)
            mediaPlayer?.release()
            mediaPlayer = null
            // Restore original volume
            originalVolume?.let { audioManager?.setStreamVolume(AudioManager.STREAM_RING, it, 0) }
        } catch (e: IllegalStateException) {
            Log.e(TAG, "IllegalStateException for MediaPlayer", e)
            mediaPlayer?.release()
            mediaPlayer = null
            // Restore original volume
            originalVolume?.let { audioManager?.setStreamVolume(AudioManager.STREAM_RING, it, 0) }
        }
    }

    @Composable
    override fun AlarmContent() {
        val bundle = remember { intent?.getBundleExtra("ALARM_DATA") }
        val calendarEvent: CalendarEvent? = remember(bundle) {
            bundle?.toCalendarEvent()
        }

        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = Color.White,
                        shape = RoundedCornerShape(32.dp),
                    )
                    .padding(32.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Trigger Icon",
                    tint = Color(0xFF111111),
                    modifier = Modifier.size(80.dp),
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (calendarEvent != null) {
                    Text(
                        text = calendarEvent.eventName,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111111),
                        textAlign = TextAlign.Center,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Time: ${
                            timeFormatter.format(
                                Date(calendarEvent.startTimeMillis),
                            )
                        } - ${
                            timeFormatter.format(
                                Date(calendarEvent.endTimeMillis),
                            )
                        }",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF333333),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Date: ${dateFormatter.format(Date(calendarEvent.startTimeMillis))}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF333333),
                        textAlign = TextAlign.Center,
                    )

                    calendarEvent.location?.takeIf { it.isNotBlank() }?.let {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Location: $it",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF555555),
                            textAlign = TextAlign.Center,
                        )
                    }

                    calendarEvent.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Description: $notes",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF555555),
                            textAlign = TextAlign.Center,
                        )
                    }

                    calendarEvent.attendees?.let { attendees ->
                        val validAttendees =
                            attendees.filter { !(it.resource ?: false) && it.email != null }
                        if (validAttendees.isNotEmpty()) {
                            Column {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Attendees",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 25.sp,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(
                                        5.dp,
                                        Alignment.CenterVertically,
                                    ),
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                ) {
                                    validAttendees.forEach { attendee ->
                                        val displayName =
                                            remember(attendee.displayName, attendee.email) {
                                                attendee.displayName ?: (
                                                    attendee.email?.split("@")
                                                        ?.first()?.split(".")
                                                        ?.first()
                                                        ?.replaceFirstChar(
                                                            Char::titlecase,
                                                        )
                                                        ?: "Unknown"
                                                    )
                                            }
                                        AssistChip(
                                            onClick = { /* No action for now */ },
                                            label = { Text(displayName, maxLines = 1) },
                                            modifier = Modifier.height(30.dp),
                                            leadingIcon = {
                                                if (attendee.organizer == true) {
                                                    Icon(
                                                        Icons.Filled.Star,
                                                        contentDescription = "Organizer",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                    )
                                                }
                                            },
                                            border = if (attendee.organizer == true) {
                                                BorderStroke(
                                                    1.dp,
                                                    MaterialTheme.colorScheme.primary,
                                                )
                                            } else {
                                                BorderStroke(1.dp, Color.White)
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Status: ${calendarEvent.eventStatus.name}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = getStatusColor(calendarEvent.eventStatus),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(50.dp))
                    Button(onClick = {
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
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Could not load event details.",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF333333),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(50.dp))
                    Button(onClick = {
                        finish()
                    }) {
                        Text(text = "STOP")
                    }
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

        originalVolume?.let { audioManager?.setStreamVolume(AudioManager.STREAM_RING, it, 0) }
    }
}

@Composable
private fun getStatusColor(status: EventStatus): Color = when (status) {
    EventStatus.PENDING -> Color(0xFFFFA726) // Orange500
    EventStatus.SCHEDULED -> Color(0xFF66BB6A) // Green400
    EventStatus.COMPLETED -> Color(0xFF808080) // Grey
    EventStatus.CANCELLED -> Color(0xFFEF5350) // Red400
}
