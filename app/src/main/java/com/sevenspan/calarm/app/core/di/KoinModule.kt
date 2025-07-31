package com.sevenspan.calarm.app.core.di

import androidx.room.Room
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.meticha.triggerx.TriggerXAlarmScheduler
import com.sevenspan.calarm.app.core.data.repository.SettingsRepository
import com.sevenspan.calarm.app.core.data.source.local.AppDatabase
import com.sevenspan.calarm.app.core.data.source.local.AppSettingsDao
import com.sevenspan.calarm.app.core.data.source.local.CalendarEventDao
import com.sevenspan.calarm.app.core.data.source.network.CalendarEventsSyncWorker
import com.sevenspan.calarm.app.core.service.AnalyticsService
import com.sevenspan.calarm.app.core.service.AuthService
import com.sevenspan.calarm.app.core.service.CalendarService
import com.sevenspan.calarm.app.core.service.FirebaseAnalyticsService
import com.sevenspan.calarm.app.core.service.FirebaseAuthService
import com.sevenspan.calarm.app.core.service.GoogleCalendarService
import com.sevenspan.calarm.app.core.service.SubscriptionService
import com.sevenspan.calarm.app.core.service.WorkScheduler
import com.sevenspan.calarm.app.features.home.di.homeModule
import com.sevenspan.calarm.app.features.signin.di.authModule
import com.sevenspan.calarm.app.features.splash.presentation.SplashViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.worker
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val appModule = module {
    single {
        Room
            .databaseBuilder(
                androidContext(),
                AppDatabase::class.java,
                "events_db",
            )
            .fallbackToDestructiveMigration(true)
            .build()
    }
    single<AnalyticsService> {
        FirebaseAnalyticsService(
            analyticsService = Firebase.analytics,
        )
    }

    single<CalendarEventDao> {
        get<AppDatabase>().calendarEventDao()
    }
    single<AppSettingsDao> {
        get<AppDatabase>().appSettingsDao()
    }

    single<AuthService> {
        FirebaseAuthService(
            settingsService = get(),
        )
    }

    single<SubscriptionService> {
        SubscriptionService(authService = get<AuthService>())
    }

    single<WorkScheduler> {
        WorkScheduler(
            context = androidContext(),
            authService = get<AuthService>(),
            subscriptionService = get<SubscriptionService>(),
        )
    }
    single<TriggerXAlarmScheduler> {
        TriggerXAlarmScheduler()
    }
    single<SettingsRepository> {
        SettingsRepository(
            settingsDao = get<AppSettingsDao>(),
            authService = get<AuthService>()
        )
    }
    single<CalendarService> {
        GoogleCalendarService(
            context = get(),
            calendarEventDao = get<CalendarEventDao>(),
            settingsRepository = get<SettingsRepository>(),
            alarmScheduler = get<TriggerXAlarmScheduler>(),
            authService = get<AuthService>(),
            subscriptionService = get<SubscriptionService>(),
        )
    }

    worker(named<CalendarEventsSyncWorker>()) {
        CalendarEventsSyncWorker(
            appContext = androidContext(),
            workerParams = get(),
            alarmScheduler = get<TriggerXAlarmScheduler>(),
            settingsRepository = get<SettingsRepository>(),
            calendarService = get<CalendarService>(),
            analytics = get<AnalyticsService>(),
            authService = get<AuthService>(),
        )
    }

    /*worker(named<CalendarSyncWorker>()) {
       CalendarSyncWorker(
           appContext = androidContext(),
           workerParams = get(),
           calendarService = get<CalendarService>(),
       )
   }*/

    viewModel {
        SplashViewModel(
            authService = get<AuthService>(),
            workScheduler = get<WorkScheduler>(),
        )
    }

    includes(authModule, homeModule)
}
