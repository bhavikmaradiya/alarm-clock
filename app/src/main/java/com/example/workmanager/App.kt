package com.example.workmanager

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.icu.util.Calendar
import android.os.Build
import android.os.Bundle
import android.util.Log
import com.example.workmanager.app.core.data.source.local.CalendarEventDao
import com.example.workmanager.app.core.di.appModule
import com.example.workmanager.app.core.domain.model.CalendarEventBundleConverter.toBundle
import com.example.workmanager.app.receiver.CalendarSyncAlarmReceiver
import com.meticha.triggerx.dsl.TriggerX
import com.meticha.triggerx.provider.TriggerXDataProvider
import org.koin.android.ext.koin.androidContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import java.util.concurrent.TimeUnit
import kotlin.jvm.java

class App : Application(), KoinComponent {


    companion object {
        const val ACTION_SYNC_CALENDAR_ALARM = "com.example.workmanager.ACTION_SYNC_CALENDAR_ALARM"
        private const val ALARM_REQUEST_CODE = 1001
        private const val TAG = "App"
        private val ALARM_INTERVAL_MILLIS = TimeUnit.HOURS.toMillis(12) // 12 hours

        // Define specific times (e.g., 8 AM and 8 PM)
        private const val SYNC_HOUR_MORNING = 8
        private const val SYNC_HOUR_EVENING = 20
    }

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@App)
            modules(appModule)
        }

        TriggerX.init(this) {
            useDefaultNotification(
                title = "Alarm running",
                message = "Tap to open",
                channelName = "Alarm Notifications"
            )

            activityClass = AlarmActivity::class.java

            /* Optional: Provide up-to-date data right before the UI opens */
            alarmDataProvider = object : TriggerXDataProvider {
                override suspend fun provideData(alarmId: Int, alarmType: String): Bundle {
                    // TODO: Implement actual data provision based on alarmId and alarmType
                    val calendarEventDao: CalendarEventDao by inject()
                    val eventFromDb = calendarEventDao.getEventById(alarmType)
                    return if (eventFromDb != null) {
                        Log.d(TAG, "Providing data for event: ${eventFromDb.eventName}")
                        eventFromDb.toBundle() // Convert your CalendarEvent to Bundle
                    } else {
                        Log.w(TAG, "No event found for alarmId: $alarmId to provide data.")
                        Bundle.EMPTY // Return an empty bundle if no event found
                    }
                    // Returning empty bundle for now
                }
            }
        }

    }

    fun scheduleInitialCalendarSyncAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Log.w(
                TAG,
                "Cannot schedule exact alarms. User permission required. Navigating to settings is recommended here."
            )
            // Optionally, navigate user to settings:
            // Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).also { context.startActivity(it) }
            return
        }

        val alarmIntent = Intent(context, CalendarSyncAlarmReceiver::class.java).apply {
            action = ACTION_SYNC_CALENDAR_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Check if alarm is already set to avoid rescheduling if not needed on simple app restart
        val isAlarmUp = PendingIntent.getBroadcast(
            context, ALARM_REQUEST_CODE, alarmIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) != null

        if (isAlarmUp) {
            Log.d(TAG, "Initial calendar sync alarm is already scheduled.")
            // return // Uncomment if you want to strictly avoid rescheduling on app start if already set.
            // BOOT_COMPLETED receiver will handle re-scheduling after reboot.
            // Or, for consistency, always cancel and reschedule to ensure the timing logic is reapplied.
        }

        // Always cancel previous before setting new one to ensure fresh timing
        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "Cancelled any existing sync alarm.")

        val nextTriggerTime = calculateNextSpecificTriggerTime()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextTriggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, nextTriggerTime, pendingIntent)
            }
            Log.d(
                TAG,
                "Initial calendar sync alarm scheduled for: ${
                    java.text.SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss",
                        java.util.Locale.getDefault()
                    ).format(nextTriggerTime)
                }"
            )
        } catch (se: SecurityException) {
            Log.e(
                TAG,
                "SecurityException while scheduling exact alarm. Check SCHEDULE_EXACT_ALARM permission.",
                se
            )
        }
    }

    fun scheduleNextCalendarSyncAlarm(context: Context, isFromReceiver: Boolean = false) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Log.w(TAG, "Cannot schedule next exact alarm. User permission required.")
            return
        }

        val alarmIntent = Intent(context, CalendarSyncAlarmReceiver::class.java).apply {
            action = ACTION_SYNC_CALENDAR_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Always cancel previous before setting new one to ensure correct chaining
        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "Cancelled any existing sync alarm before scheduling next.")

        val nextTriggerTime: Long
        if (isFromReceiver) {
            // If called from receiver, schedule 12 hours from now
            nextTriggerTime = System.currentTimeMillis() + ALARM_INTERVAL_MILLIS
        } else {
            // If called from elsewhere (e.g. initial setup), calculate specific time
            nextTriggerTime = calculateNextSpecificTriggerTime()
        }


        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextTriggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, nextTriggerTime, pendingIntent)
            }
            Log.d(
                TAG,
                "Next calendar sync alarm scheduled by ${if (isFromReceiver) "receiver" else "app"} for: ${
                    java.text.SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss",
                        java.util.Locale.getDefault()
                    ).format(nextTriggerTime)
                }"
            )
        } catch (se: SecurityException) {
            Log.e(TAG, "SecurityException while scheduling next exact alarm.", se)
        }
    }

    private fun calculateNextSpecificTriggerTime(): Long {
        val now = Calendar.getInstance()
        val currentTimeMillis = now.timeInMillis

        val morningAlarm = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, SYNC_HOUR_MORNING)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (morningAlarm.timeInMillis <= currentTimeMillis) { // If 8 AM already passed today
            morningAlarm.add(Calendar.DAY_OF_YEAR, 1) // Schedule for 8 AM tomorrow
        }

        val eveningAlarm = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, SYNC_HOUR_EVENING)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (eveningAlarm.timeInMillis <= currentTimeMillis) { // If 8 PM already passed today
            eveningAlarm.add(Calendar.DAY_OF_YEAR, 1) // Schedule for 8 PM tomorrow
        }

        // Choose the earliest of the two future alarms
        return if (morningAlarm.timeInMillis < eveningAlarm.timeInMillis) {
            if (morningAlarm.timeInMillis > currentTimeMillis) morningAlarm.timeInMillis else eveningAlarm.timeInMillis
        } else {
            if (eveningAlarm.timeInMillis > currentTimeMillis) eveningAlarm.timeInMillis else morningAlarm.timeInMillis
        }
    }
}
