package com.bhavikm.calarm.app.features.home.di

import com.bhavikm.calarm.app.core.data.repository.SettingsRepository
import com.bhavikm.calarm.app.core.data.source.network.CalendarEventsSyncWorker
import com.bhavikm.calarm.app.core.service.AnalyticsService
import com.bhavikm.calarm.app.core.service.AuthService
import com.bhavikm.calarm.app.core.service.CalendarService
import com.bhavikm.calarm.app.core.service.WorkScheduler
import com.bhavikm.calarm.app.features.home.data.repository.HomeRepository
import com.bhavikm.calarm.app.features.home.data.repository.HomeRepositoryImpl
import com.bhavikm.calarm.app.features.home.presentation.HomeViewModel
import com.meticha.triggerx.TriggerXAlarmScheduler
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.worker
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val homeModule = module {
    factory<HomeRepository> {
        HomeRepositoryImpl(
            calendarService = get<CalendarService>()
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

    viewModel {
        HomeViewModel(
            repository = get<HomeRepository>(),
            settingsRepository = get<SettingsRepository>(),
            alarmScheduler = get<TriggerXAlarmScheduler>(),
            workScheduler = get<WorkScheduler>(),
        )
    }
}
