package com.example.workmanager.app.core.di


import androidx.room.Room
import com.example.workmanager.app.core.data.source.local.AppDatabase
import com.example.workmanager.app.features.home.di.homeModule
import com.example.workmanager.app.features.signin.di.authModule
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module


val appModule = module {
    single {
        Room
            .databaseBuilder(
                androidContext(),
                AppDatabase::class.java,
                "events_db",
            ).build()
    }
    single { get<AppDatabase>().calendarEventDao() }
    single { get<AppDatabase>().appSettingsDao() }
    includes(homeModule, authModule)
}
