package com.example.workmanager.app.features.home.presentation

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.workmanager.app.core.domain.model.CalendarEvent
import com.example.workmanager.app.core.domain.model.EventStatus
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.ExponentialBackOff
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.CalendarScopes
import com.meticha.triggerx.permission.rememberAppPermissionState
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.text.format

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val permissionState = rememberAppPermissionState()
    LaunchedEffect(Unit) {
        if (permissionState.allRequiredGranted()) {
            viewModel.getCalendar(context)
        }
    }
    HomeComposable(homeSate = state)
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Body(
    modifier: Modifier = Modifier,
    homeSate: HomeState,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val permissionState = rememberAppPermissionState()

    Box(
        modifier
            .fillMaxSize()
            .background(
                color = Color.LightGray.copy(alpha = 0.3f),
            ),
    ) {

        when (homeSate.status) {
            HomeStatus.INITIAL -> {
                Button(modifier = Modifier.align(Alignment.Center), onClick = {
                    if (permissionState.allRequiredGranted()) {
                        viewModel.getCalendar(context)
                    } else {
                        permissionState.requestPermission()
                    }
                }) {
                    Text(text = "Fetch and schedule event")
                }
            }

            HomeStatus.LOADING -> {
                Text(modifier = Modifier.align(Alignment.Center), text = "Loading")
            }

            HomeStatus.LOADED  -> {
                LazyColumn {
                    items(homeSate.events) { event -> // eventsList is your List<CalendarEvent>
                        EventItem(event = event)
                    }
                }
            }

            HomeStatus.ERROR   -> {
                Text(
                    modifier = Modifier.align(Alignment.Center),
                    text = homeSate.error ?: "Something went wrong!"
                )
            }

            HomeStatus.EMPTY   -> {
                Text(modifier = Modifier.align(Alignment.Center), text = "No data found")
            }

        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
fun HomePreview() {
    HomeComposable()
}

// Simple Date Formatters (You can place these at the top of your file or in a utility object)
private val timeFormatter = SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
private val dateFormatter = SimpleDateFormat(
    "EEE, MMM d, yyyy",
    java.util.Locale.getDefault()
)

@Composable
fun EventItem(event: CalendarEvent, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status indicator for the app's local event status
            EventStatusIndicator(status = event.eventStatus)

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.eventName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Start and End Time
                Text(
                    text = "Time: ${timeFormatter.format(Date(event.startTimeMillis))} - ${
                        timeFormatter.format(
                            Date(event.endTimeMillis)
                        )
                    }",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Date: ${dateFormatter.format(Date(event.startTimeMillis))}",
                    style = MaterialTheme.typography.bodySmall
                )


                // Location (if available)
                event.location?.let {
                    if (it.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Location: $it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Google Calendar API Status (optional, for debugging or info)
                event.googleCalendarApiStatus?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "API Status: $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                // Displaying the app's local EventStatus as text
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "App Status: ${event.eventStatus.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = getStatusColor(event.eventStatus).copy(alpha = 0.8f) // Use the status color
                )

                // Recurring event indicator (optional)
                if (event.isRecurring) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Recurring Event",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@Composable
fun EventStatusIndicator(status: EventStatus, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(10.dp)
            .background(color = getStatusColor(status), shape = MaterialTheme.shapes.small)
    )
}

// Helper function to get a color based on EventStatus
// You should customize these colors to fit your app's theme
@Composable
private fun getStatusColor(status: EventStatus): Color {
    return when (status) {
        EventStatus.PENDING   -> MaterialTheme.colorScheme.secondaryContainer
        EventStatus.SCHEDULED -> MaterialTheme.colorScheme.primaryContainer
        EventStatus.COMPLETED -> MaterialTheme.colorScheme.tertiaryContainer
        EventStatus.CANCELLED -> MaterialTheme.colorScheme.errorContainer
        // Add more states if your EventStatus enum has them
    }
}
