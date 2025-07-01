package com.bhavikm.calarm

import android.app.NotificationManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Bundle
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
            audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
            originalVolume = audioManager?.getStreamVolume(AudioManager.STREAM_RING)

            val notificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            if (notificationManager.isNotificationPolicyAccessGranted) {
                val maxVolume = audioManager?.getStreamMaxVolume(AudioManager.STREAM_RING)
                maxVolume?.let {
                    audioManager?.setStreamVolume(AudioManager.STREAM_RING, it, 0)
                }
            } else {
                Log.w(TAG, "DND access not granted; skipping volume change")
            }

            val notificationSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

            if (notificationSoundUri != null) {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(this@AlarmActivity, notificationSoundUri)
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    isLooping = true
                    setOnPreparedListener { start() }
                    setOnCompletionListener {
                        releaseMediaPlayer()
                    }
                    setOnErrorListener { mp, what, extra ->
                        Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                        releaseMediaPlayer()
                        true
                    }
                    prepareAsync()
                }
            } else {
                Log.w(TAG, "Default ringtone URI is null.")
                restoreOriginalVolume()
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error setting up MediaPlayer", e)
            releaseMediaPlayer()
        } catch (e: IllegalStateException) {
            Log.e(TAG, "IllegalStateException", e)
            releaseMediaPlayer()
        }
    }

    private fun releaseMediaPlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
        restoreOriginalVolume()
    }

    private fun restoreOriginalVolume() {
        originalVolume?.let {
            audioManager?.setStreamVolume(AudioManager.STREAM_RING, it, 0)
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
                    imageVector = Icons.Default.AlarmOn,
                    contentDescription = "Trigger Icon",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(80.dp),
                )

                Spacer(modifier = Modifier.height(18.dp))

                if (calendarEvent != null) {
                    Text(
                        text = calendarEvent.eventName,
                        fontSize = 27.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(35.dp))

                    // Row for Date and Time
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        EventDetailItem(
                            iconVector = Icons.Filled.CalendarMonth,
                            label = "Date",
                            value = dateFormatter.format(Date(calendarEvent.startTimeMillis)),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        EventDetailItem(
                            iconVector = Icons.Filled.Schedule,
                            label = "Time",
                            value = "${timeFormatter.format(Date(calendarEvent.startTimeMillis))} - ${
                                timeFormatter.format(Date(calendarEvent.endTimeMillis))
                            }",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        EventDetailItem(
                            iconVector = Icons.Filled.LocationOn,
                            label = "Location",
                            value = calendarEvent.location?.takeIf { it.isNotBlank() }
                                    ?: "Not specified",
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        val attendee = calendarEvent.attendees?.find { it.organizer == true }

                        val hostValue =
                            attendee?.displayName ?: (
                                attendee?.email?.split("@")
                                    ?.first()?.split(".")
                                    ?.first()
                                    ?.replaceFirstChar(Char::titlecase)
                                ?: "Unknown"
                                                     )
                        EventDetailItem(
                            iconVector = Icons.Filled.Person,
                            label = "Host",
                            value = hostValue,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    calendarEvent.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                        Spacer(modifier = Modifier.height(16.dp))
                        Row {
                            EventDetailItem(
                                iconVector = Icons.Filled.Notes,
                                label = "Notes",
                                value = notes,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    calendarEvent.attendees?.let { attendees ->
                        val validAttendees =
                            attendees.filter { !(it.resource ?: false) && it.email != null }
                        if (validAttendees.isNotEmpty()) {
                            val values = validAttendees.mapIndexed { i, attendee ->
                                val displayName = (
                                                      attendee.displayName ?: (
                                                          attendee.email?.split("@")
                                                              ?.first()?.split(".")
                                                              ?.first()
                                                              ?.replaceFirstChar(
                                                                  Char::titlecase,
                                                              )
                                                          ?: "Unknown"
                                                                              )
                                                  ) +
                                                  if (i !=
                                                      validAttendees.size -
                                                      1
                                                  ) {
                                                      ","
                                                  } else {
                                                      ""
                                                  }
                                return@mapIndexed displayName
                            }.joinToString(" ")
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Spacer(modifier = Modifier.height(16.dp))

                                Row {
                                    EventDetailItem(
                                        iconVector = Icons.Filled.People,
                                        label = "Attendees",
                                        value = values,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                            }
                        }
                    }
                } else {
                    Text(
                        text = "Unavailable",
                        fontSize = 27.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Could not load event details.",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF333333),
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(modifier = Modifier.height(35.dp))
                Button(
                    onClick = { finish() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "STOP")
                }
            }
        }
    }

    @Composable
    private fun EventDetailItem(
        iconVector: ImageVector,
        label: String,
        value: String,
        modifier: Modifier = Modifier,
    ) {
        Column(modifier = modifier, horizontalAlignment = Alignment.Start) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = label,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    fontSize = 15.sp,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Medium,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 15.sp,
                style = MaterialTheme.typography.bodyMedium,
            )
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
        restoreOriginalVolume()
    }
}

@Composable
private fun getStatusColor(status: EventStatus): Color = when (status) {
    EventStatus.PENDING -> Color(0xFFFFA726) // Orange500
    EventStatus.SCHEDULED -> Color(0xFF66BB6A) // Green400
    EventStatus.COMPLETED -> Color(0xFF808080) // Grey
    EventStatus.CANCELLED -> Color(0xFFEF5350) // Red400
}
