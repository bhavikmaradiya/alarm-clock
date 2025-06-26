package com.bhavikm.calarm.app.features.home.presentation

import android.annotation.SuppressLint
import android.icu.util.Calendar
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bhavikm.calarm.app.core.model.CalendarEvent
import com.bhavikm.calarm.app.core.model.EventStatus
import com.meticha.triggerx.permission.rememberAppPermissionState
import kotlinx.datetime.Clock
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun HomeScreen(viewModel: HomeViewModel = koinViewModel()) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val uiEvent = viewModel.uiEvent
    val permissionState = rememberAppPermissionState()
    val snackBarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        uiEvent.collect { event ->
            when (event) {
                is HomeUIEvent.ScheduledEvent -> {
                    snackBarHostState.showSnackbar("Event scheduled successfully!")
                }

                else                          -> {}
            }
        }
    }

    LaunchedEffect(key1 = permissionState.allRequiredGranted()) {
        if (permissionState.allRequiredGranted()) {
            viewModel.getCalendar(context)
        }
    }

    HomeComposable(homeSate = state, snackBarHostState = snackBarHostState)
}

@Composable
fun Body(
    homeSate: HomeState,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val permissionState = rememberAppPermissionState()

    Column(
        modifier
            .fillMaxSize()
            .background(
                color = Color.LightGray.copy(alpha = 0.3f),
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Column {
                Text(
                    text = "Load your calendar events:",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(end = 16.dp),
                )
                if (homeSate.lastSynced != null) {
                    Text(
                        text = "Last synced: ${formatLastSyncedTime(homeSate.lastSynced)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Button(onClick = {
                if (permissionState.allRequiredGranted()) {
                    viewModel.getCalendar(context)
                } else {
                    permissionState.requestPermission()
                }
            }) {
                Text(text = "Sync")
            }
        }
        Box(modifier = Modifier.fillMaxSize()) {
            when (homeSate.status) {
                HomeStatus.LOADING -> {
                    Text(modifier = Modifier.align(Alignment.Center), text = "Loading")
                }

                HomeStatus.LOADED  -> {
                    LazyColumn {
                        items(homeSate.events) { event ->
                            // eventsList is your List<CalendarEvent>
                            EventItem(event = event)
                        }
                    }
                }

                HomeStatus.ERROR   -> {
                    Text(
                        modifier = Modifier.align(Alignment.Center),
                        text = homeSate.error ?: "Something went wrong!",
                    )
                }

                HomeStatus.INITIAL,
                HomeStatus.EMPTY,
                                   -> {
                    Text(
                        modifier = Modifier.align(Alignment.Center),
                        text = "No data found!\nTry to sync again!",
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

private fun formatLastSyncedTime(timestamp: Long?): String {
    if (timestamp == null || timestamp <= 0) {
        return "Never"
    }

    val currentTime = System.currentTimeMillis()
    val diffMillis = currentTime - timestamp

    val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis)
    val hours = TimeUnit.MILLISECONDS.toHours(diffMillis)

    val calendarNow = Calendar.getInstance()
    val calendarTimestamp = Calendar.getInstance().apply { timeInMillis = timestamp }

    return when {
        minutes < 1                                 -> "Just now"
        minutes < 60                                -> if (minutes == 1L) {
            "$minutes minute ago"
        } else {
            "$minutes minutes ago"
        }

        hours < 24 &&
        calendarNow.get(Calendar.DAY_OF_YEAR) == calendarTimestamp.get(Calendar.DAY_OF_YEAR) &&
        calendarNow.get(
            Calendar.YEAR,
        ) == calendarTimestamp.get(Calendar.YEAR)   -> {
            val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
            "${sdf.format(Date(timestamp))}"
        }

        isYesterday(calendarTimestamp, calendarNow) -> {
            val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
            "Yesterday at ${sdf.format(Date(timestamp))}"
        }

        else                                        -> {
            val sdf = SimpleDateFormat("MMM dd, yyyy h:mm a", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}

private fun isYesterday(targetCalendar: Calendar, currentCalendar: Calendar): Boolean {
    val tempCalendar = currentCalendar.clone() as Calendar
    tempCalendar.add(Calendar.DAY_OF_YEAR, -1)
    return tempCalendar.get(Calendar.YEAR) == targetCalendar.get(Calendar.YEAR) &&
           tempCalendar.get(
               Calendar.DAY_OF_YEAR,
           ) == targetCalendar.get(Calendar.DAY_OF_YEAR)
}

@Composable
private fun HomeComposable(
    homeSate: HomeState = HomeState(),
    snackBarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackBarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Events") },
            )
        },
    ) { innerPadding ->
        Body(
            modifier = Modifier.padding(innerPadding),
            homeSate = homeSate,
        )
    }
}

@Preview
@Composable
private fun HomePreview() {
    HomeComposable()
}

// Simple Date Formatters (You can place these at the top of your file or in a utility object)
private val timeFormatter = SimpleDateFormat("h:mm a", Locale.getDefault())
private val dateFormatter = SimpleDateFormat(
    "EEE, MMM d, yyyy",
    Locale.getDefault(),
)

private fun formatRemainingTime(startTimeMillis: Long): String {
    val currentTimeMillis = Clock.System.now().toEpochMilliseconds()
    val remainingMillis = startTimeMillis - currentTimeMillis

    if (remainingMillis <= 0) {
        return "Now"
    }

    val minutes = TimeUnit.MILLISECONDS.toMinutes(remainingMillis)
    val hours = TimeUnit.MILLISECONDS.toHours(remainingMillis)
    val days = TimeUnit.MILLISECONDS.toDays(remainingMillis)

    val calendarStart = Calendar.getInstance().apply { timeInMillis = startTimeMillis }
    val calendarNow = Calendar.getInstance()

    return when {
        minutes < 60                                                                                         -> "In $minutes min"
        hours < 24 && calendarStart.get(Calendar.DAY_OF_YEAR) == calendarNow.get(Calendar.DAY_OF_YEAR)       -> "In $hours hr"
        days.toInt() == 0 && calendarStart.get(Calendar.DAY_OF_YEAR) > calendarNow.get(Calendar.DAY_OF_YEAR) -> { // Tomorrow
            val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
            "Tomorrow at ${sdf.format(Date(startTimeMillis))}"
        }

        else                                                                                                 -> {
            // More than tomorrow, show day, hour, and minute
            val remainingHoursInDay = hours % 24
            "In $days d $remainingHoursInDay hr"
        }
    }

}

@SuppressLint("UnusedContentLambdaTargetStateParameter")
@Composable
fun EventItem(event: CalendarEvent, modifier: Modifier = Modifier) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 20.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { expanded = !expanded },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {

        Row(
            modifier = Modifier.padding(12.dp),
//            verticalAlignment = Alignment.CenterVertically,
        ) {

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f),
            )
            {
                Row(
                    modifier = Modifier.height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = formatRemainingTime(event.startTimeMillis),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.alignByBaseline()
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    "event.location"?.let {
                        if (it.isNotBlank()) {
                            Text(
                                text = "(at $it)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.alignByBaseline()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = event.eventName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                AnimatedVisibility(visible = expanded) {
                    Column {
                        Spacer(modifier = Modifier.height(15.dp))
                        event.notes?.let {
                            if (it.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Description: $it",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(7.dp))

                        event.attendees?.let { attendees ->
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
                                        fontSize = 22.sp,
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
                                        validAttendees.forEachIndexed { i, attendee ->
                                            val displayName =
                                                remember(attendee.displayName, attendee.email) {
                                                    (
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
                                                }
                                            Box {
                                                Text(displayName, maxLines = 1)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            androidx.compose.material3.Icon(
                imageVector = if (expanded) {
                    Icons.Filled.KeyboardArrowUp
                } else {
                    Icons.Filled.KeyboardArrowDown
                },
                contentDescription = if (expanded) "Collapse" else "Expand",
                modifier = Modifier
                    .size(30.dp)
                    .padding(start = 8.dp), // Add some padding if needed
                tint = MaterialTheme.colorScheme.onSurfaceVariant // Optional: Set icon color
            )
        }
    }
}


@Composable
fun EventStatusIndicator(status: EventStatus, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(10.dp)
            .background(color = getStatusColor(status), shape = MaterialTheme.shapes.small),
    )
}

// Helper function to get a color based on EventStatus
// You should customize these colors to fit your app's theme
@Composable
private fun getStatusColor(status: EventStatus): Color = when (status) {
    EventStatus.PENDING   -> MaterialTheme.colorScheme.secondaryContainer
    EventStatus.SCHEDULED -> MaterialTheme.colorScheme.primaryContainer
    EventStatus.COMPLETED -> MaterialTheme.colorScheme.tertiaryContainer
    EventStatus.CANCELLED -> MaterialTheme.colorScheme.errorContainer
}
