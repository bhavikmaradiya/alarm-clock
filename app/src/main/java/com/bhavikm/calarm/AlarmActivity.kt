package com.bhavikm.calarm

import android.app.NotificationManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Bundle
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import com.bhavikm.calarm.app.core.model.CalendarEvent
import com.bhavikm.calarm.app.core.model.CalendarEventBundleConverter.toCalendarEvent
import com.google.android.material.textview.MaterialTextView
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import com.google.firebase.auth.auth
import com.meticha.triggerx.TriggerXActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val timeFormatter = SimpleDateFormat("h:mm a", Locale.getDefault())
private val dateFormatter = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
private const val TAG = "AlarmActivity"

class AlarmActivity : TriggerXActivity() {

    private var originalVolume: Int? = null
    private lateinit var audioManager: AudioManager
    lateinit var ringtone: Ringtone

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
        audioManager = getSystemService(
            AudioManager::
            class.java
        )
        originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING)

        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.isNotificationPolicyAccessGranted) {
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
            val halfVolume = maxVolume / 2
            audioManager.setStreamVolume(AudioManager.STREAM_RING, halfVolume, 0)
        }

        val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        ringtone = RingtoneManager.getRingtone(applicationContext, ringtoneUri)
        ringtone.audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE) // This maps to STREAM_RING
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        ringtone.play()
    }


    private fun restoreOriginalVolume() {
        originalVolume?.let {
            audioManager.setStreamVolume(AudioManager.STREAM_RING, it, 0)
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
                            ) {
                                ExpandableHtmlText(
                                    notes,
                                    collapsedMaxLines = 5,
                                )
                            }
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
    fun ExpandableHtmlText(
        htmlText: String,
        collapsedMaxLines: Int = 3,
    ) {
        var isExpanded by remember { mutableStateOf(false) }
        var isTruncated by remember { mutableStateOf(false) }

        Column {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth(),
                factory = { context ->
                    MaterialTextView(context).apply {
                        setTextColor(android.graphics.Color.BLACK)
                        movementMethod = LinkMovementMethod.getInstance()
                        ellipsize = TextUtils.TruncateAt.END
                        text = HtmlCompat.fromHtml(htmlText, HtmlCompat.FROM_HTML_MODE_LEGACY)
                        maxLines = if (isExpanded) Int.MAX_VALUE else collapsedMaxLines

                        post {
                            val layout = layout ?: return@post
                            val didOverflow = layout.lineCount > collapsedMaxLines
                            if (didOverflow != isTruncated) {
                                isTruncated = didOverflow
                            }
                        }
                    }
                },
                update = {
                    it.text = HtmlCompat.fromHtml(htmlText, HtmlCompat.FROM_HTML_MODE_LEGACY)
                    it.maxLines = if (isExpanded) Int.MAX_VALUE else collapsedMaxLines
                    it.setOnClickListener {
                        if (isTruncated || isExpanded) {
                            isExpanded = !isExpanded
                        }
                    }
                }
            )

            AnimatedVisibility(visible = isTruncated || isExpanded) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .clickable { isExpanded = !isExpanded },
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isExpanded) "Show less" else "Show more",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
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
        content: @Composable (() -> Unit)? = null,
    ) {
        var isNotesExpanded by rememberSaveable { mutableStateOf(false) }
        var textIsTruncated by remember { mutableStateOf(false) }
        val collapsedMaxLines = 5
        val interactionSource = remember { MutableInteractionSource() }
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
            content?.invoke() ?: Column(
                // Make the whole notes section clickable to toggle expansion
                modifier = Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {

                    // Only toggle if there's actually more to show or if it's already expanded
                    if (textIsTruncated || isNotesExpanded) {
                        isNotesExpanded = !isNotesExpanded
                    }
                }
            ) {
                AnimatedVisibility(visible = !isNotesExpanded) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 15.sp,

                        maxLines = collapsedMaxLines,
                        overflow = TextOverflow.Ellipsis,
                        onTextLayout = { textLayoutResult ->
                            textIsTruncated = textLayoutResult.didOverflowHeight
                        }
                    )
                }
                AnimatedVisibility(visible = isNotesExpanded) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 15.sp
                    )
                }

                // Conditionally show the "Show more/less" indicator
                AnimatedVisibility(visible = textIsTruncated || isNotesExpanded) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isNotesExpanded) "Show less" else "Show more",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Icon(
                            imageVector = if (isNotesExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = if (isNotesExpanded) "Collapse" else "Expand",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }


    override fun onDestroy() {
        ringtone.stop()
        restoreOriginalVolume()
        super.onDestroy()
    }
}
