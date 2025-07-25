package com.sevenspan.calarm.app.features.home.di

import com.meticha.triggerx.TriggerXAlarmScheduler
import com.sevenspan.calarm.app.core.data.repository.SettingsRepository
import com.sevenspan.calarm.app.core.data.source.network.CalendarEventsSyncWorker
import com.sevenspan.calarm.app.core.service.AuthService
import com.sevenspan.calarm.app.core.service.CalendarService
import com.sevenspan.calarm.app.core.service.CalendarSyncWorker
import com.sevenspan.calarm.app.core.service.WorkScheduler
import com.sevenspan.calarm.app.features.home.data.repository.HomeRepository
import com.sevenspan.calarm.app.features.home.data.repository.HomeRepositoryImpl
import com.sevenspan.calarm.app.features.home.presentation.HomeViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.worker
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val homeModule = module {
    factory<HomeRepository> {
        HomeRepositoryImpl(
            calendarService = get<CalendarService>(),
            authService = get<AuthService>(),
        )
    }

    /*worker(named<CalendarEventsSyncWorker>()) {
        CalendarEventsSyncWorker(
            appContext = androidContext(),
            workerParams = get(),
            alarmScheduler = get<TriggerXAlarmScheduler>(),
            settingsRepository = get<SettingsRepository>(),
            calendarService = get<CalendarService>(),
            analytics = get<AnalyticsService>(),
            authService = get<AuthService>(),
        )
    }*/

    worker(named<CalendarEventsSyncWorker>()) {
        CalendarSyncWorker(
            appContext = androidContext(),
            workerParams = get(),
            calendarService = get<CalendarService>(),
        )
    }

    viewModel {
        HomeViewModel(
            homeRepository = get<HomeRepository>(),
            settingsRepository = get<SettingsRepository>(),
            alarmScheduler = get<TriggerXAlarmScheduler>(),
            workScheduler = get<WorkScheduler>(),
            signInRepository = get(),
        )
    }
}
