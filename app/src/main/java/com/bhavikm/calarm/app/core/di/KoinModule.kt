package com.bhavikm.calarm.app.core.di

import androidx.room.Room
import com.bhavikm.calarm.app.core.data.repository.SettingsRepository
import com.bhavikm.calarm.app.core.data.source.local.AppDatabase
import com.bhavikm.calarm.app.core.data.source.local.AppSettingsDao
import com.bhavikm.calarm.app.core.data.source.local.CalendarEventDao
import com.bhavikm.calarm.app.core.service.AnalyticsService
import com.bhavikm.calarm.app.core.service.AuthService
import com.bhavikm.calarm.app.core.service.CalendarService
import com.bhavikm.calarm.app.core.service.FirebaseAnalyticsService
import com.bhavikm.calarm.app.core.service.FirebaseAuthService
import com.bhavikm.calarm.app.core.service.GoogleCalendarService
import com.bhavikm.calarm.app.core.service.WorkScheduler
import com.bhavikm.calarm.app.features.home.di.homeModule
import com.bhavikm.calarm.app.features.signin.di.authModule
import com.bhavikm.calarm.app.features.splash.presentation.SplashViewModel
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.meticha.triggerx.TriggerXAlarmScheduler
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
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
    single<AuthService> {
        FirebaseAuthService(androidContext())
    }
    single<WorkScheduler> {
        WorkScheduler(context = androidContext(), authService = get<AuthService>())
    }
    single<CalendarEventDao> { get<AppDatabase>().calendarEventDao() }
    single<AppSettingsDao> { get<AppDatabase>().appSettingsDao() }
    single<TriggerXAlarmScheduler> { TriggerXAlarmScheduler() }
    single<SettingsRepository> {
        SettingsRepository(settingsDao = get<AppSettingsDao>(), authService = get<AuthService>())
    }
    single<CalendarService> {
        GoogleCalendarService(
            context = get(),
            calendarEventDao = get<CalendarEventDao>(),
            settingsRepository = get<SettingsRepository>(),
            alarmScheduler = get<TriggerXAlarmScheduler>(),
            authService = get<AuthService>(),
        )
    }
    viewModel {
        SplashViewModel(
            authService = get<AuthService>(),
        )
    }

    includes(homeModule, authModule)
}
