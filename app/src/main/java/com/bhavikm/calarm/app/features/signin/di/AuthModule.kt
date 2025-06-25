package com.bhavikm.calarm.app.features.signin.di

import com.bhavikm.calarm.app.core.data.source.local.AppSettingsDao
import com.bhavikm.calarm.app.core.service.AuthService
import com.bhavikm.calarm.app.features.signin.data.repository.SignInRepositoryImpl
import com.bhavikm.calarm.app.features.signin.domain.repository.SignInRepository
import com.bhavikm.calarm.app.features.signin.presentation.SignInViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val authModule = module {
    factory<SignInRepository> {
        SignInRepositoryImpl(
            appSettingsDao = get<AppSettingsDao>(),
            authService = get<AuthService>(),
        )
    }

    viewModel {
        SignInViewModel(get<SignInRepository>())
    }
}
