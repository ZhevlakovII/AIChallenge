package ru.izhxx.aichallenge

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ru.izhxx.aichallenge.ui.ChatScreen
import ru.izhxx.aichallenge.ui.SettingsScreen

/**
 * Главные экраны приложения
 */
sealed class Screen(val route: String) {
    object Chat : Screen("chat")
    object Settings : Screen("settings")
}

/**
 * Константы для отступов в UI
 */
object AppDimens {
    val baseContentPadding = 16.dp
}

@Composable
fun App() {
    MaterialTheme {
        val navController = rememberNavController()

        // Получаем системные отступы
        val navigationBarInsets = WindowInsets.navigationBars

        // Оборачиваем все в Box с общими отступами, учитывающими системные инсеты
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                // Применяем отступы со всех сторон с учетом системных инсетов
                .padding(
                    // Низ: учитываем навигационную панель плюс базовый отступ
                    bottom = navigationBarInsets.asPaddingValues().calculateBottomPadding() + AppDimens.baseContentPadding,
                )
        ) {
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
