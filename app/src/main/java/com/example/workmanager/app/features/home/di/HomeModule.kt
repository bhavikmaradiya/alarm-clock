package com.example.workmanager.app.features.home.di

import com.example.workmanager.app.core.data.source.local.AppSettingsDao
import com.example.workmanager.app.core.data.source.local.CalendarEventDao
import com.example.workmanager.app.features.home.data.repository.HomeRepositoryImpl
import com.example.workmanager.app.features.home.domain.repository.HomeRepository
import com.example.workmanager.app.features.home.presentation.HomeViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val homeModule = module {
    factory<HomeRepository> {
        HomeRepositoryImpl(
            context = get(),
            calendarEventDao = get<CalendarEventDao>(), appSettingsDao = get<AppSettingsDao>()
        )
    }

    viewModel {
        HomeViewModel(
            get<HomeRepository>()
        )
    }
}