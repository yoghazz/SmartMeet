package com.smartmeet.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.smartmeet.app.ui.screens.auth.LoginScreen
import com.smartmeet.app.ui.screens.auth.RegisterScreen
import com.smartmeet.app.ui.screens.dashboard.DashboardScreen
import com.smartmeet.app.ui.screens.document.DocumentScreen
import com.smartmeet.app.ui.screens.library.LibraryScreen
import com.smartmeet.app.ui.screens.session.NewSessionScreen
import com.smartmeet.app.ui.screens.session.RecordingScreen
import com.smartmeet.app.ui.screens.session.SessionDetailScreen
import com.smartmeet.app.ui.screens.settings.SettingsScreen
import com.smartmeet.app.ui.screens.splash.SplashScreen

object Routes {
    const val Splash = "splash"
    const val Login = "login"
    const val Register = "register"
    const val Dashboard = "dashboard"
    const val Library = "library"
    const val Settings = "settings"
    const val NewSession = "new_session"
    const val Recording = "recording/{sessionId}"
    const val SessionDetail = "session_detail/{sessionId}"
    const val Documents = "documents/{sessionId}"
}

@Composable
fun SmartMeetNavGraph() {
    val navController = rememberNavController()

    fun goLogin() = navController.navigate(Routes.Login) {
        popUpTo(0) { inclusive = true }
    }

    NavHost(navController = navController, startDestination = Routes.Splash) {
        composable(Routes.Splash) {
            SplashScreen(onNavigateNext = { loggedIn ->
                val dest = if (loggedIn) Routes.Dashboard else Routes.Login
                navController.navigate(dest) { popUpTo(Routes.Splash) { inclusive = true } }
            })
        }
        composable(Routes.Login) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.Dashboard) { popUpTo(Routes.Login) { inclusive = true } }
                },
                onRegisterClick = { navController.navigate(Routes.Register) }
            )
        }
        composable(Routes.Register) {
            RegisterScreen(
                onRegisterSuccess = { navController.popBackStack() },
                onLoginClick = { navController.popBackStack() }
            )
        }
        composable(Routes.Dashboard) {
            DashboardScreen(
                onNewSession = { navController.navigate(Routes.NewSession) },
                onOpenSettings = { navController.navigate(Routes.Settings) },
                onOpenLibrary = { navController.navigate(Routes.Library) },
                onOpenSession = { id -> navController.navigate("session_detail/$id") }
            )
        }
        composable(Routes.NewSession) {
            NewSessionScreen(
                onBack = { navController.popBackStack() },
                onSessionCreated = { id -> navController.navigate("recording/$id") }
            )
        }
        composable(
            Routes.Recording,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) {
            RecordingScreen(
                onBack = { navController.popBackStack() },
                onStop = { id ->
                    navController.navigate("session_detail/$id") {
                        popUpTo("recording/$id") { inclusive = true }
                    }
                }
            )
        }
        composable(
            Routes.SessionDetail,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) {
            SessionDetailScreen(
                onBack = { navController.popBackStack() },
                onOpenDocuments = { id -> navController.navigate("documents/$id") }
            )
        }
        composable(
            Routes.Documents,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) {
            DocumentScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.Library) {
            LibraryScreen(
                onBack = { navController.popBackStack() },
                onOpenSession = { id -> navController.navigate("session_detail/$id") }
            )
        }
        composable(Routes.Settings) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onLoggedOut = { goLogin() }
            )
        }
    }
}
