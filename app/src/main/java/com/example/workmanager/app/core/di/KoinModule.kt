package com.example.workmanager.app.core.di


import androidx.room.Room
import com.example.workmanager.app.core.data.source.local.AppDatabase
import com.example.workmanager.app.features.home.di.homeModule
import com.example.workmanager.app.features.signin.di.authModule
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import kotlin.jvm.java


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
    single<FirebaseUser?> { Firebase.auth.currentUser }
    includes(homeModule, authModule)
}
