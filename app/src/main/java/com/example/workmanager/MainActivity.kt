package com.example.workmanager

import android.app.AlarmManager
import android.app.AppOpsManager
import android.app.KeyguardManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit
import androidx.core.net.toUri
import com.example.workmanager.AlarmScheduler.requestExactAlarmPermission
import java.lang.reflect.Method

class MainActivity : ComponentActivity() {
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
        requestIgnoreBatteryOptimizations(this)
        setContent {
            val context = LocalContext.current
            var scheduled by remember { mutableStateOf(false) }

            MaterialTheme {
                Scaffold {
                    Button(modifier = Modifier.padding(it), onClick = {
                        if (AlarmScheduler.canScheduleExactAlarms(context)) {
                            AlarmScheduler.scheduleAlarm(context)
                        } else {
                            requestExactAlarmPermission(context)
                        }
                        scheduled = true
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

    private fun isShowOnLockScreenPermissionEnable(context: Context): Boolean {
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

    private fun requestIgnoreBatteryOptimizations(context: Context) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val packageName = context.packageName

        if (isShowOnLockScreenPermissionEnable(context)) {
            Toast.makeText(this, "Permission is already granted!!", Toast.LENGTH_SHORT).show()
        } else {
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

        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = "package:$packageName".toUri()
            }
            context.startActivity(intent)
        }
    }


    /*  private fun schedulePreAlarmWorker(context: Context) {
          val scheduledTimeMillis =
              System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5) // 5 min later
          val preAlarmTimeMillis =
              scheduledTimeMillis - TimeUnit.MINUTES.toMillis(4)          // 4 min before
          val delay = preAlarmTimeMillis - System.currentTimeMillis()

          if (delay <= 0) {
              println("Pre-alarm time is in the past. Not scheduling.")
              return
          } // already past

          val request = OneTimeWorkRequestBuilder<OverlayWorker>()
              .setInitialDelay(delay, TimeUnit.MILLISECONDS)
              .build()

          WorkManager.getInstance(context).enqueue(request)
          println("Pre-alarm scheduled for $preAlarmTimeMillis")
      }*/
}

@Composable
fun HomeScreen(onScheduleClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🏠 Home Screen", fontSize = 24.sp)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onScheduleClick) {
            Text("Schedule Pre-Alarm")
        }
    }
}

@Composable
fun PreAlarmScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("⏰ Pre-Alarm Screen", fontSize = 24.sp)
    }
}

object AlarmScheduler {
    fun scheduleAlarm(context: Context) {
        val scheduledTimeMillis =
            System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5) // 5 min later
        val alarmTime = scheduledTimeMillis - 4 * 60 * 1000  // 4 minutes before

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

    fun scheduleAlarmClock(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val scheduledTimeMillis =
            System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5) // 5 min later
        val alarmTime = scheduledTimeMillis - 4 * 60 * 1000  // 4 minutes before
        val alarmIntent = Intent(context, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val showIntent = PendingIntent.getActivity(
            context,
            0,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmClockInfo = AlarmManager.AlarmClockInfo(alarmTime, showIntent)

        // This does NOT need receiver — it launches the activity directly
        alarmManager.setAlarmClock(alarmClockInfo, showIntent)
        println("scheduleAlarmClock Alarm scheduled for $alarmTime")
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
}


/*
class PreAlarmWorker(
    context: Context,
    workerParams: WorkerParameters,
) : Worker(context, workerParams) {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun doWork(): Result {
        val context = applicationContext
        val serviceIntent = Intent(context, OverlayForegroundService::class.java)
        context.startForegroundService(serviceIntent)
        return Result.success()
    }
}*/
