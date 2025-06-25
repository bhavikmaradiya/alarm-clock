package com.bhavikm.calarm.app.features.settings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen() {
    var notificationsEnabled by remember { mutableStateOf(true) }
    var expandedReminderDelay by remember { mutableStateOf(false) }
    val reminderDelayOptions = listOf("1 min ago", "5 min ago", "20 min ago")
    var selectedReminderDelay by remember { mutableStateOf(reminderDelayOptions[0]) }

    var expandedCalendarFetchDays by remember { mutableStateOf(false) }
    val calendarFetchDaysOptions = listOf("1 day", "2 days")
    var selectedCalendarFetchDays by remember { mutableStateOf(calendarFetchDaysOptions[0]) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings") })
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Notification Toggle Setting
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Enable Notifications",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = { notificationsEnabled = it },
                )
            }

            HorizontalDivider()

            // Reminder Delay Setting
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Reminder Delay",
                    style = MaterialTheme.typography.bodyLarge,
                )
                ExposedDropdownMenuBox(
                    expanded = expandedReminderDelay,
                    onExpandedChange = { expandedReminderDelay = !expandedReminderDelay },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    TextField(
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        readOnly = true,
                        value = selectedReminderDelay,
                        onValueChange = {},
                        label = { Text("Delay before trigger") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = expandedReminderDelay,
                            )
                        },
                        colors = ExposedDropdownMenuDefaults.textFieldColors(),
                    )
                    ExposedDropdownMenu(
                        expanded = expandedReminderDelay,
                        onDismissRequest = { expandedReminderDelay = false },
                    ) {
                        reminderDelayOptions.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption) },
                                onClick = {
                                    selectedReminderDelay = selectionOption
                                    expandedReminderDelay = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            // Calendar Fetch Days Setting
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Fetch Calendar Events For",
                    style = MaterialTheme.typography.bodyLarge,
                )
                ExposedDropdownMenuBox(
                    expanded = expandedCalendarFetchDays,
                    onExpandedChange = { expandedCalendarFetchDays = !expandedCalendarFetchDays },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    TextField(
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        readOnly = true,
                        value = selectedCalendarFetchDays,
                        onValueChange = {},
                        label = { Text("Days from now") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = expandedCalendarFetchDays,
                            )
                        },
                        colors = ExposedDropdownMenuDefaults.textFieldColors(),
                    )
                    ExposedDropdownMenu(
                        expanded = expandedCalendarFetchDays,
                        onDismissRequest = { expandedCalendarFetchDays = false },
                    ) {
                        calendarFetchDaysOptions.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption) },
                                onClick = {
                                    selectedCalendarFetchDays = selectionOption
                                    expandedCalendarFetchDays = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    MaterialTheme {
        // Assuming you have a MaterialTheme set up
        SettingsScreen()
    }
}
