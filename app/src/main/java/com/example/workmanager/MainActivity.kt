package com.example.workmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import com.example.workmanager.navigation.Navigation
import com.example.workmanager.ui.theme.WorkManagerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EventApp()
        }
    }
}

@Composable
private fun EventApp() {
    WorkManagerTheme {
        Navigation()
    }
}

/*class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                "package:$packageName".toUri()
            )
            startActivity(intent)
        }
        requestNotificationPermission()
        requestShowOnLockScreenPermission(this)
//        requestIgnoreBatteryOptimizations(this)
        setContent {
            val context = LocalContext.current
            var scheduled by remember { mutableStateOf(false) }

            MaterialTheme {
                Scaffold {
                    Button(modifier = Modifier.padding(it), onClick = {
                        val canScheduleExactAlarms = AlarmScheduler.canScheduleExactAlarms(context)
                        val canDrawOverlays = Settings.canDrawOverlays(this)
                        val hasIgnoreBatteryOptimizations = hasIgnoreBatteryOptimizations(this)
                        val isShowOnLockScreenPermissionEnable =
                            isShowOnLockScreenPermissionEnable(context)
                        if (canDrawOverlays && isShowOnLockScreenPermissionEnable && canScheduleExactAlarms && hasIgnoreBatteryOptimizations
                        ) {
                            AlarmScheduler.scheduleAlarm(context)
                            scheduled = true
                        } else if (!isShowOnLockScreenPermissionEnable) {
                            Toast.makeText(
                                context,
                                "Show on lock screen permission is not enable",
                                Toast.LENGTH_SHORT
                            ).show()
                            requestShowOnLockScreenPermission(context)
                        } else if (!canScheduleExactAlarms) {
                            Toast.makeText(
                                context,
                                "Exact alarm permission is not enable",
                                Toast.LENGTH_SHORT
                            ).show()
                            requestExactAlarmPermission(context)
                        } else if (!hasIgnoreBatteryOptimizations) {
                            Toast.makeText(
                                context,
                                "Ignore battery optimizations permission is not enable",
                                Toast.LENGTH_SHORT
                            ).show()
                            requestIgnoreBatteryOptimizations(context)
                        } else {
                            Toast.makeText(
                                context,
                                "Draw overlays permission is not enable",
                                Toast.LENGTH_SHORT
                            ).show()
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                "package:$packageName".toUri()
                            )
                            startActivity(intent)
                        }

                    }) {
                        Text(if (scheduled) "Work Scheduled" else "Schedule Pre-alarm Work")
                    }
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                )
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }

    }

    @SuppressLint("DiscouragedPrivateApi")
    private fun isShowOnLockScreenPermissionEnable(context: Context): Boolean {
        if (!Build.MANUFACTURER.equals("Xiaomi", true)) {
            return true
        }
        return try {
            val manager = context.getSystemService(APP_OPS_SERVICE) as AppOpsManager
            val method: Method = AppOpsManager::class.java.getDeclaredMethod(
                "checkOpNoThrow",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                String::class.java
            )
            val result =
                method.invoke(manager, 10020, Binder.getCallingUid(), context.packageName) as Int
            AppOpsManager.MODE_ALLOWED == result
        } catch (e: Exception) {
            false
        }
    }

    private fun requestShowOnLockScreenPermission(context: Context) {
        if (!isShowOnLockScreenPermissionEnable(context)) {
            if (Build.MANUFACTURER.equals("Xiaomi", true)) {
                val intent = Intent("miui.intent.action.APP_PERM_EDITOR")
                intent.setClassName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.permissions.PermissionsEditorActivity"
                )
                intent.putExtra("extra_pkgname", packageName)
                startActivity(intent)
            }
        }
    }

    @SuppressLint("BatteryLife")
    private fun requestIgnoreBatteryOptimizations(context: Context) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val packageName = context.packageName

        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = "package:$packageName".toUri()
            }
            context.startActivity(intent)
        }
    }

    private fun hasIgnoreBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(POWER_SERVICE) as PowerManager
        val packageName = context.packageName
        return powerManager.isIgnoringBatteryOptimizations(packageName)
    }
}

object AlarmScheduler {
    fun scheduleAlarm(context: Context) {
        val scheduledTimeMillis =
            System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5) // 5 min later
//        val alarmTime = scheduledTimeMillis - 4 * 60 * 1000  // 4 minutes before
        val alarmTime = 40000L  // 4 minutes before

        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val alarmClockInfo = AlarmManager.AlarmClockInfo(alarmTime, pendingIntent)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent
        )
        println("scheduleAlarm Alarm scheduled for $alarmTime")
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun requestExactAlarmPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = "package:${context.packageName}".toUri()
            }
            context.startActivity(intent)
        }
    }
}*/
