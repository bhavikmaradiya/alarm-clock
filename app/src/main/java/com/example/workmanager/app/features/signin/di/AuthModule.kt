package com.example.workmanager.app.features.signin.di


import com.example.workmanager.app.core.data.source.local.AppSettingsDao
import com.example.workmanager.app.features.signin.data.repository.SignInRepositoryImpl
import com.example.workmanager.app.features.signin.domain.repository.SignInRepository
import com.example.workmanager.app.features.signin.domain.usecase.GetCredentialsUseCase
import com.example.workmanager.app.features.signin.domain.usecase.SignInUseCases
import com.example.workmanager.app.features.signin.presentation.SignInViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val authModule = module {
    factory<SignInRepository> {
        SignInRepositoryImpl(get<AppSettingsDao>())
    }

    factory<SignInUseCases> {
        SignInUseCases(
            getCredentialsUseCase = GetCredentialsUseCase(get()),
        )
    }

    viewModel {
        SignInViewModel(get())
    }
}