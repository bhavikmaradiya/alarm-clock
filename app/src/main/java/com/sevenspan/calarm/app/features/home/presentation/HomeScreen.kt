package com.sevenspan.calarm.app.features.home.presentation

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.icu.util.Calendar
import android.provider.Settings
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.core.text.HtmlCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.google.android.material.textview.MaterialTextView
import com.meticha.triggerx.permission.PermissionState
import com.meticha.triggerx.permission.rememberAppPermissionState
import com.sevenspan.calarm.CalarmApp.Companion.isNetworkAvailable
import com.sevenspan.calarm.app.core.model.AttendeeData
import com.sevenspan.calarm.app.core.model.CalendarEvent
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

@Composable
fun HomeScreen(viewModel: HomeViewModel = koinViewModel(), onSignOut: () -> Unit) {
    val context = LocalActivity.current as Activity
    val state by viewModel.state.collectAsStateWithLifecycle()
    val uiEvent = viewModel.uiEvent
    val permissionState = rememberAppPermissionState()
    val snackBarHostState = remember { SnackbarHostState() }
    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    var isNotificationListenerDialogVisible by remember {
        mutableStateOf(
            !isNotificationListenerEnabled(
                context
            )
        )
    }

    val googleCalendarScopeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { result -> viewModel.processResult(context, result) }


    LaunchedEffect(Unit) {
        if (!notificationManager.isNotificationPolicyAccessGranted
        ) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            context.startActivity(intent)
            snackBarHostState.showSnackbar("Please grant sound access for Calarm to work properly.")
        }

        uiEvent.collect {
            when (val event = it) {
                is HomeUIEvent.ScheduledEvent          -> {
                    snackBarHostState.showSnackbar("Event scheduled successfully!")
                }

                is HomeUIEvent.OnSignInIntentGenerated -> {
                    if (event.intent != null) {
                        googleCalendarScopeLauncher.launch(
                            IntentSenderRequest.Builder(event.intent.intentSender).build()
                        )
                    }
                }

                is HomeUIEvent.OnSignInFailure         -> {
                    snackBarHostState.showSnackbar("Sign in failed. Please try again.")
                    onSignOut.invoke()
                }

                else                                   -> {}
            }
        }
    }

    NotificationAccessDialog(
        showDialog = isNotificationListenerDialogVisible,
        onDismiss = {
            isNotificationListenerDialogVisible = false
        }
    )

    LaunchedEffect(key1 = Unit) {
        if (permissionState.allRequiredGranted()) {
            viewModel.getCalendar(context)
        }
    }

    HomeComposable(
        context = context,
        homeSate = state,
        snackBarHostState = snackBarHostState,
        permissionState = permissionState,
        onSyncClick = {
            viewModel.getCalendar(context)
        },
        onSwitchAccountClick = {
            viewModel.switchAccount(context)
        }
    )
}

fun isNotificationListenerEnabled(context: Context): Boolean {
    val packageName = context.packageName
    val enabledListeners = Settings.Secure.getString(
        context.contentResolver,
        "enabled_notification_listeners"
    ) ?: return false

    return enabledListeners.split(":").any { it.contains(packageName) }
}

@Composable
fun Body(
    homeSate: HomeState,
    modifier: Modifier = Modifier,
) {
    LocalContext.current
    rememberAppPermissionState()

    Column(
        modifier
            .fillMaxSize()
    ) {
        /*Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        )
        {
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
            Button(
                onClick = {
                    if (permissionState.allRequiredGranted()) {
                        viewModel.getCalendar(context)
                    } else {
                        permissionState.requestPermission()
                    }
                },
            ) {
                Text(text = "Sync")
            }
        }*/
        Box(modifier = Modifier.fillMaxSize()) {
            when (homeSate.status) {
                HomeStatus.LOADING -> {
                    LoadingIndicator(
                        modifier = Modifier.align(alignment = Alignment.Center),
                        color = Color.Gray,
                    )
                }

                HomeStatus.LOADED  -> {
                    LazyColumn {
                        items(homeSate.events) { event ->
                            EventItem(
                                event = event,
                                modifier = Modifier.padding(
                                    vertical = 12.dp,
                                    horizontal = 20.dp
                                )
                            )
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

@Composable
private fun AttendeeInitialsCircle(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
) {
    val initial = name.firstOrNull()?.uppercaseChar() ?: '?'
    Box(
        modifier = modifier
            .size(size)
            .background(rememberAvatarBackgroundColor(name = name), CircleShape)
            .clip(CircleShape)
            .border(width = 1.5.dp, color = Color.White, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial.toString(),
            color = Color.White, // Assuming white text for contrast
            fontSize = (size.value / 2).sp, // Dynamic font size
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun rememberAvatarBackgroundColor(name: String): Color {
    val themeColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
    )
    return themeColors[abs(name.hashCode()) % themeColors.size]
}

@Composable
fun NotificationAccessDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            ),
            title = {
                Text("Calendar Updates")
            },
            text = {
                Text(
                    "Want to stay updated even when you're busy or on the move? \n\n" +
                    "Allow notification access so we can sync your calendar updates even when you're on hustle."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val context = context
                    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    onDismiss()
                }) {
                    Text("Allow Access")
                }
            },
        )
    }
}


@Composable
private fun StackedAttendeesAvatars(
    attendees: List<AttendeeData>,
    modifier: Modifier = Modifier,
    avatarSize: Dp = 24.dp,
    overlapFactor: Float = 0.37f,
    maxVisible: Int = 3,
) {
    val validAttendees = attendees
        .filter { !(it.resource ?: false) && (it.displayName != null || it.email != null) }
        .take(maxVisible + 1) // Take one more to check if we need to show "+N"

    if (validAttendees.isEmpty()) return

    Box(modifier = modifier) {
        validAttendees.take(maxVisible).forEachIndexed { index, attendee ->
            val name = attendee.displayName ?: attendee.email ?: "Unknown"
            AttendeeInitialsCircle(
                name = name,
                size = avatarSize,
                modifier = Modifier
                    .padding(start = (index * avatarSize * (1 - overlapFactor)))
                    .zIndex(maxVisible - index.toFloat()) // Higher zIndex for items at the start of the list (drawn on top)
            )
        }

        if (validAttendees.size > maxVisible) {
            val remainingCount =
                attendees.size - maxVisible // Calculate based on original full list
            Box(
                modifier = Modifier
                    .padding(start = (maxVisible * avatarSize * (1 - overlapFactor)))
                    .size(avatarSize)
                    .background(
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        CircleShape
                    )
                    .clip(CircleShape)
                    .zIndex(0f), // Ensure it's behind the last visible avatar if needed, or drawn last
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+$remainingCount",
                    color = Color.White,
                    fontSize = (avatarSize.value / 2.5).sp,
                    fontWeight = FontWeight.Medium
                )
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

private fun isYesterday(
    targetCalendar: Calendar,
    currentCalendar: Calendar,
): Boolean {
    val tempCalendar = currentCalendar.clone() as Calendar
    tempCalendar.add(Calendar.DAY_OF_YEAR, -1)
    return tempCalendar.get(Calendar.YEAR) == targetCalendar.get(Calendar.YEAR) &&
           tempCalendar.get(
               Calendar.DAY_OF_YEAR,
           ) == targetCalendar.get(Calendar.DAY_OF_YEAR)
}

@Composable
private fun HomeComposable(
    context: Context,
    homeSate: HomeState = HomeState(),
    permissionState: PermissionState,
    snackBarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onSyncClick: () -> Unit = {},
    onSwitchAccountClick: () -> Unit = {},
) {
    val isLoading = homeSate.status == HomeStatus.LOADING
    val scope = rememberCoroutineScope()
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackBarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text("Events") },
                    colors = TopAppBarDefaults.topAppBarColors().copy(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
                    actions = {
                        FilledTonalIconButton(
                            content = {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = "Sync",
                                )
                            },
                            onClick = {
                                scope.launch {
                                    if (permissionState.allRequiredGranted() && isNetworkAvailable(
                                            context
                                        )
                                    ) {
                                        onSyncClick.invoke()
                                    } else if (!permissionState.allRequiredGranted()) {
                                        permissionState.requestPermission()
                                    } else {
                                        snackBarHostState.showSnackbar("Please check your internet connection")
                                    }
                                }
                            },
                            colors = IconButtonDefaults.filledTonalIconButtonColors()
                                .copy(containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(end = 10.dp)
                        )
                        AsyncImage(
                            model = homeSate.userData?.photoUrl,
                            contentDescription = "User Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .clip(CircleShape)
                                .border(
                                    5.dp,
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                                    CircleShape
                                )
                                .clickable(true) {
                                    onSwitchAccountClick.invoke()
                                },
                        )
                    }
                )
            },
        ) { innerPadding ->
            Body(
                modifier = Modifier.padding(innerPadding),
                homeSate = homeSate,
            )
        }
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { }
                    },
                contentAlignment = Alignment.Center
            ) {}
        }
    }
}


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
        minutes < 60                                                                                         -> "In $minutes minutes"
        hours < 24 && calendarStart.get(Calendar.DAY_OF_YEAR) == calendarNow.get(Calendar.DAY_OF_YEAR)       -> "In $hours hours"
        days.toInt() == 0 && calendarStart.get(Calendar.DAY_OF_YEAR) > calendarNow.get(Calendar.DAY_OF_YEAR) -> { // Tomorrow
            val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
            "Tomorrow at ${sdf.format(Date(startTimeMillis))}"
        }

        else                                                                                                 -> {
            val remainingHoursInDay = hours % 24
            "In $days day ${if (remainingHoursInDay > 0) "$remainingHoursInDay hours" else ""}"
        }
    }

}

@Composable
fun EventItem(
    event: CalendarEvent,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 20.dp, horizontal = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f),
            )
            {
                Text(
                    text = formatRemainingTime(event.startTimeMillis),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 26.sp,
                )

                Spacer(modifier = Modifier.width(3.dp))

                event.location?.let {
                    if (it.isNotBlank()) {
                        Text(
                            text = "at $it",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = event.eventName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )

                AnimatedVisibility(visible = expanded) {
                    Column {
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Duration",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Time: ${timeFormatter.format(Date(event.startTimeMillis))} - ${
                                timeFormatter.format(
                                    Date(event.endTimeMillis),
                                )
                            }",
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = "Date: ${
                                dateFormatter.format(Date(event.startTimeMillis))
                            }",
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 14.sp
                        )
                        event.notes?.let { notes ->
                            if (notes.isNotBlank()) {
                                val collapsedMaxLines = 5
                                remember { MutableInteractionSource() }

                                Spacer(modifier = Modifier.height(17.dp))

                                Text(
                                    text = "Notes",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 16.sp,
                                )
                                Spacer(modifier = Modifier.height(4.dp))

                                ExpandableHtmlText(
                                    notes,
                                    collapsedMaxLines = collapsedMaxLines,
                                )
                            }
                        }

                        event.attendees?.let { attendees ->
                            val validAttendees =
                                attendees.filter { !(it.resource ?: false) && it.email != null }
                            if (validAttendees.isNotEmpty()) {
                                Column {
                                    Spacer(modifier = Modifier.height(17.dp))
                                    Text(
                                        text = "Attendees",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 16.sp,
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
                                                remember(
                                                    attendee.displayName,
                                                    attendee.email
                                                ) {
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
                                                Text(
                                                    displayName,
                                                    maxLines = 1,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontSize = 14.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxHeight()
            ) {
                Icon(
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
                if (!expanded && !event.attendees.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    StackedAttendeesAvatars(attendees = event.attendees)
                }
            }
        }
    }
}

@Composable
fun ExpandableHtmlText(
    htmlText: String,
    collapsedMaxLines: Int = 3,
) {
    remember { MutableInteractionSource() }
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
