package com.sevenspan.calarm.app.features.faq.presentation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class FaqItem(
    val question: String,
    val answer: String,
    val intentAction: String? = null,
    val isProtectedAppSetting: Boolean = false, // Special flag for ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaqScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val faqItems = listOf(
        FaqItem(
            question = "Why does Calarm need Notification Listener access?",
            answer = "Calarm needs Notification Listener access to detect when other apps (like your calendar app) post notifications for new or updated events. This allows Calarm to automatically schedule or update alarms based on those notifications, ensuring your alarms are always in sync with your calendar without you needing to open Calarm.",
            intentAction = Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
        ),
        FaqItem(
            question = "Why does Calarm need Do Not Disturb (DND) access / Notification Policy access?",
            answer = "To ensure your alarms are audible even when your device is in Do Not Disturb mode or Silent mode, Calarm requires permission to override DND. This is crucial for critical alarms that you don'''t want to miss.",
            intentAction = Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS
        ),
        FaqItem(
            question = "Why does Calarm need general Notification permissions?",
            answer = "Calarm uses notifications to alert you for upcoming events (your alarms). It also uses notifications for important alerts, such as if a permission is missing or if there'''s an issue with syncing. Without this permission, you won'''t receive any alarm notifications.",
            intentAction = Settings.ACTION_APP_NOTIFICATION_SETTINGS,
        ),
        FaqItem(
            question = "Why does Calarm need Calendar access?",
            answer = "Calarm reads your calendar to display your upcoming events and allow you to easily set alarms for them. This is a core feature of the app. Calarm only requests read-only access and does not modify your calendar events.",
            // No direct intent action, user grants this via system dialog, usually at first launch or feature use.
        ),
        FaqItem(
            question = "Why does Calarm need 'Display over other apps' permission?",
            answer = """This permission allows Calarm to show its alarm interface and other important alerts directly on your screen, even if you are using another app or if your phone is locked. For instance, when an alarm is due, Calarm needs to display its interactive alarm screen on top of whatever is currently visible. This ensures you see and can respond to your alarms immediately. Without this, alarms might only appear as standard notifications, which can be easily missed.

Manual steps to enable:
1. Open your device'''s **Settings** app.
2. Search for or navigate to **'Special app access'**. This might be under 'Apps', 'Privacy', or its own section.
3. Find and tap on **'Display over other apps'** (or 'Appear on top', 'Draw over other apps').
4. Locate **Calarm** in the list and enable the permission.
If you can'''t find it, try searching settings for 'overlay' or 'draw over'.""",
            intentAction = Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            isProtectedAppSetting = false
        ),
        FaqItem(
            question = "Alarms don't appear from background even if 'Display over other apps' is on?",
            answer = """If you've granted 'Display over other apps' permission but alarms **still don't appear** when Calarm tries to trigger them from the background, the issue often lies with your phone's battery optimization or manufacturer-specific background restrictions. These systems can prevent apps like Calarm from running or launching UI from the background to save power.

**What to check:**
1.  **The 'Display over other apps' permission (as described in the FAQ above) MUST be enabled first.**
2.  Then, go to Calarm's main 'App Info' page in your phone's settings (the 'Open Settings' button below can take you there).
3.  Look for sections related to:
    *   **Battery Usage / Battery Optimization:** Ensure Calarm is set to 'Unrestricted', 'Not optimized', or an equivalent that allows background activity.
    *   **Background Activity / App background settings:** Explicitly allow Calarm to run in the background.
    *   **Autostart / App Launch / Startup Manager:** If your phone has such a feature, make sure Calarm is enabled to autostart or launch automatically when needed.
    *   **'Protected Apps' / 'App Lock' (in recent apps):** Some systems require you to 'lock' an app in the recent apps screen to prevent it from being closed.

The exact naming and location of these settings vary greatly between phone manufacturers (Xiaomi, Huawei, Oppo, Samsung, OnePlus, etc.). You might need to explore your device's specific settings under 'Apps', 'Battery', or 'Device Maintenance'.""",
            intentAction = Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            isProtectedAppSetting = false
        ),
        FaqItem(
            question = "My alarms are not ringing or are delayed. What can I do?",
            answer = """This can often be due to battery optimization settings or background restrictions imposed by your phone'''s manufacturer (especially on devices from Xiaomi, Huawei, Oppo, OnePlus, etc.).

Please ensure Calarm is:
1. Allowed to run in the background without restrictions.
2. Excluded from battery optimization.
3. Enabled for Autostart if your device has such a setting.

You can usually find these settings under Battery settings or App Management. The exact steps vary by device. Look for sections like 'Protected Apps', 'Battery Optimization', 'App Launch', or 'Autostart'. Some phones require you to 'lock' the app in the recent apps screen to prevent it from being killed.""",
            isProtectedAppSetting = true // This uses ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
        ),
        FaqItem(
            question = "How do I enable permissions if I denied them initially?",
            answer = """You can typically manage app permissions through your phone'''s settings. The exact path can vary slightly depending on your Android version and phone manufacturer, but the general steps are:

1.  **Open your device'''s Settings app.** (Usually a gear icon).
2.  **Find the 'Apps' or 'Application Manager' or 'Manage Apps' section.** Sometimes this is directly on the main settings page, or it might be under a 'General' or 'Device' category.
3.  **Find and tap on 'Calarm'** in the list of installed applications. You might need to select 'See all apps' if it'''s not immediately visible.
4.  **Tap on 'Permissions'.** This section will list all the permissions the app can request.
5.  **Review each permission Calarm needs.** If a permission is denied, tap on it.
6.  **Select 'Allow' or 'Allow only while using the app'.** Choose the appropriate option. For permissions Calarm needs to function correctly (like Calendar, Notifications), 'Allow' is generally recommended.
7.  **For special permissions like Notification Listener or Do Not Disturb access:**
    *   **Notification Listener:** You might find this under Settings > Apps > Special app access > Notification access. Ensure Calarm is toggled ON. Alternatively, use the "Open Settings" button in the specific FAQ above.
    *   **Do Not Disturb (Notification Policy):** This might be under Settings > Apps > Special app access > Do Not Disturb access. Ensure Calarm is allowed. Alternatively, use the "Open Settings" button in the specific FAQ above.

If you have trouble finding these settings, you can also often long-press the Calarm app icon on your home screen or app drawer, tap on the 'App info' (often an 'i' in a circle) icon, and then find the 'Permissions' section from there.""",
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FAQ & Permissions") },
                colors = TopAppBarDefaults.topAppBarColors().copy(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(faqItems) { faq ->
                FaqCard(faqItem = faq, context = context)
            }
        }
    }
}

@Composable
fun FaqCard(faqItem: FaqItem, context: Context) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = faqItem.question,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                )
            }
            AnimatedVisibility(visible = isExpanded) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = faqItem.answer,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                    if (faqItem.intentAction != null || faqItem.isProtectedAppSetting) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                try {
                                    val intent: Intent
                                    if (faqItem.isProtectedAppSetting) { // Specifically for ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                                        intent =
                                            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                                data = Uri.parse("package:${context.packageName}")
                                            }
                                    } else if (faqItem.intentAction != null) {
                                        intent = Intent(faqItem.intentAction)
                                        // Add package context for specific intents that require it
                                        when (faqItem.intentAction) {
                                            Settings.ACTION_APP_NOTIFICATION_SETTINGS -> {
                                                intent.putExtra(
                                                    Settings.EXTRA_APP_PACKAGE,
                                                    context.packageName
                                                )
                                                // Note: ACTION_APP_NOTIFICATION_SETTINGS may also need EXTRA_CHANNEL_ID for specific channels on API 26+
                                                // but for general app notification settings, EXTRA_APP_PACKAGE is key.
                                            }

                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                                                                      -> {
                                                intent.data =
                                                    Uri.parse("package:${context.packageName}")
                                            }
                                            // Other intents like ACTION_NOTIFICATION_LISTENER_SETTINGS or
                                            // ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS typically don't
                                            // require package-specific data in the same way.
                                        }
                                    } else {
                                        // Should not happen if button is shown, but as a safeguard:
                                        return@Button
                                    }
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(intent)
                                } catch (e: ActivityNotFoundException) {
                                    // Fallback if the specific intent fails, especially for ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                                    if (faqItem.isProtectedAppSetting) {
                                        try {
                                            val fallbackIntent =
                                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                    data =
                                                        Uri.parse("package:${context.packageName}")
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                            context.startActivity(fallbackIntent)
                                        } catch (e2: Exception) {
                                            // Handle if even app details settings cannot be opened (e.g., show Toast)
                                        }
                                    } else {
                                        // Handle for other intents if needed (e.g., show Toast "Could not open settings")
                                    }
                                } catch (e: Exception) {
                                    // General exception handling (e.g., show Toast "Could not open settings")
                                }
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Open Settings")
                        }
                    }
                }
            }
        }
    }
}
