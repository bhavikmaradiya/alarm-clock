package com.sevenspan.calarm.app.features.home.di

import com.meticha.triggerx.TriggerXAlarmScheduler
import com.sevenspan.calarm.app.core.data.repository.SettingsRepository
import com.sevenspan.calarm.app.core.service.AuthService
import com.sevenspan.calarm.app.core.service.CalendarService
import com.sevenspan.calarm.app.core.service.WorkScheduler
import com.sevenspan.calarm.app.features.home.data.repository.HomeRepository
import com.sevenspan.calarm.app.features.home.data.repository.HomeRepositoryImpl
import com.sevenspan.calarm.app.features.home.presentation.HomeViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val homeModule = module {
    factory<HomeRepository> {
        HomeRepositoryImpl(
            calendarService = get<CalendarService>(),
            authService = get<AuthService>(),
        )
    }

    viewModel {
        HomeViewModel(
            homeRepository = get<HomeRepository>(),
            settingsRepository = get<SettingsRepository>(),
            alarmScheduler = get<TriggerXAlarmScheduler>(),
            workScheduler = get<WorkScheduler>(),
            signInRepository = get(),
            subscriptionService = get()
        )
    }
}
