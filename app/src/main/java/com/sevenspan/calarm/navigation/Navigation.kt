package com.sevenspan.calarm.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSavedStateNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.rememberSceneSetupNavEntryDecorator
import com.sevenspan.calarm.app.features.home.presentation.HomeScreen
import com.sevenspan.calarm.app.features.settings.presentation.SettingsScreen
import com.sevenspan.calarm.app.features.signin.presentation.SignInScreen
import com.sevenspan.calarm.app.features.splash.presentation.SplashScreen
import kotlinx.serialization.Serializable

sealed class Routes : NavKey {
    @Serializable
    data object SplashRoute : Routes()

    @Serializable
    data object HomeRoute : Routes()

    @Serializable
    data object SignInRoute : Routes()

    @Serializable
    data object SettingsRoute : Routes()
}

@Composable
fun Navigation() {
    val backStack = rememberNavBackStack<Routes>(Routes.SplashRoute)
    NavDisplay(
        backStack = backStack,
        entryDecorators = listOf(
            rememberSavedStateNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
            rememberSceneSetupNavEntryDecorator(),
        ),
        onBack = {
            backStack.removeLastOrNull()
        },
        entryProvider = entryProvider {
            entry<Routes.SplashRoute> {
                SplashScreen { isAuthenticated ->
                    if (isAuthenticated) {
                        backStack.replaceAll { Routes.HomeRoute }
                    } else {
                        backStack.replaceAll { Routes.SignInRoute }
                    }
                }
            }
            entry<Routes.SignInRoute> {
                SignInScreen {
                    backStack.replaceAll { Routes.HomeRoute }
                }
            }
            entry<Routes.HomeRoute> {
                HomeScreen() {
                    backStack.replaceAll { Routes.SignInRoute }
                }
            }
            entry<Routes.SettingsRoute> {
                SettingsScreen()
            }
        },
    )
}
