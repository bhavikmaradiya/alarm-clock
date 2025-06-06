package com.example.workmanager.navigation

// Import for SplashScreen - will be created in the next step
// import com.example.workmanager.app.features.splash.presentation.SplashScreen
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSavedStateNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.rememberSceneSetupNavEntryDecorator
import com.example.workmanager.app.features.home.presentation.HomeScreen
import com.example.workmanager.app.features.signin.presentation.SignInScreen
import com.example.workmanager.app.features.splash.presentation.SplashScreen
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.serialization.Serializable

sealed class Routes : NavKey {
    @Serializable
    data object SplashRoute : Routes()

    @Serializable
    data object HomeRoute : Routes()

    @Serializable
    data object SignInRoute : Routes()
}

@Composable
fun Navigation() {
    val backStack = rememberNavBackStack<Routes>(Routes.SplashRoute)
    NavDisplay(
        backStack = backStack,
        entryDecorators = listOf(
            rememberSavedStateNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
            rememberSceneSetupNavEntryDecorator()
        ),
        onBack = {
            backStack.removeLastOrNull()
        },
        entryProvider = entryProvider {
            entry<Routes.SplashRoute> {
                SplashScreen(currentUser = Firebase.auth.currentUser) { isAuthenticated ->
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
                HomeScreen()
            }
        }
    )
}
