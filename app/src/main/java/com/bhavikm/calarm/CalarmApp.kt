package com.bhavikm.calarm

import android.app.Application
import android.os.Bundle
import android.util.Log
import com.bhavikm.calarm.app.core.data.source.local.CalendarEventDao
import com.bhavikm.calarm.app.core.di.appModule
import com.bhavikm.calarm.app.core.model.CalendarEventBundleConverter.toBundle
import com.meticha.triggerx.dsl.TriggerX
import com.meticha.triggerx.provider.TriggerXDataProvider
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin

class CalarmApp :
    Application(),
    KoinComponent {

    companion object Companion {
        private const val TAG = "App"
    }

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@CalarmApp)
            workManagerFactory()
            modules(appModule)
        }

        TriggerX.init(this) {
            useDefaultNotification(
                title = "Alarm running",
                message = "Tap to open",
                channelName = "Alarm Notifications",
            )

            activityClass = AlarmActivity::class.java

            alarmDataProvider = object : TriggerXDataProvider {
                override suspend fun provideData(alarmId: Int, alarmType: String): Bundle {
                    Log.d(TAG, "Providing data for alarmId: $alarmId, alarmType: $alarmType")
                    // TODO: Implement actual data provision based on alarmId and alarmType
                    val calendarEventDao: CalendarEventDao by inject()
                    val eventFromDb = calendarEventDao.getEventById(alarmId)
                    return if (eventFromDb != null) {
                        Log.d(TAG, "Providing data for event: ${eventFromDb.eventName}")
                        eventFromDb.toBundle() // Convert your CalendarEvent to Bundle
                    } else {
                        Log.w(TAG, "No event found for alarmId: $alarmId to provide data.")
                        Bundle.EMPTY // Return an empty bundle if no event found
                    }
                }
            }
        }
    }
}
