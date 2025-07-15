package com.sevenspan.calarm.app.features.signin.di

import com.sevenspan.calarm.app.core.data.source.local.AppSettingsDao
import com.sevenspan.calarm.app.core.service.AuthService
import com.sevenspan.calarm.app.features.signin.data.repository.SignInRepositoryImpl
import com.sevenspan.calarm.app.features.signin.domain.repository.SignInRepository
import com.sevenspan.calarm.app.features.signin.presentation.SignInViewModel
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
