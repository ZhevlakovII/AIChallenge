package ru.izhxx.aichallenge

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import org.koin.compose.KoinContext
import ru.izhxx.aichallenge.ui.ChatScreen
import ru.izhxx.aichallenge.ui.SettingsScreen

/**
 * Главные экраны приложения
 */
sealed class Screen(val route: String) {
    object Chat : Screen("chat")
    object Settings : Screen("settings")
}

@Composable
fun App() {
    // Оборачиваем приложение в KoinContext для доступа к Koin DI
    KoinContext {
        MaterialTheme {
            val navController = rememberNavController()
            
            NavHost(navController = navController, startDestination = Screen.Chat.route) {
                // Экран чата
                composable(Screen.Chat.route) {
                    ChatScreen(
                        onNavigateToSettings = {
                            navController.navigate(Screen.Settings.route)
                        }
                    )
                }
                
                // Экран настроек
                composable(Screen.Settings.route) {
                    SettingsScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }
}
