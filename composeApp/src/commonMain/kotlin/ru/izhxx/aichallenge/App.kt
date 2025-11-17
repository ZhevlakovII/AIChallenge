package ru.izhxx.aichallenge

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ru.izhxx.aichallenge.features.metrics.ChatMetricsScreen
import ru.izhxx.aichallenge.features.chat.presentation.ChatScreen
import ru.izhxx.aichallenge.features.settings.SettingsScreen
import ru.izhxx.aichallenge.features.mcp.presentation.McpScreen

/**
 * Главные экраны приложения
 */
sealed class Screen(val route: String) {
    object Chat : Screen("chat")
    object Settings : Screen("settings")
    object Metrics : Screen("metrics")
    object Mcp : Screen("mcp")
}

/**
 * Константы для отступов в UI
 */
object AppDimens {
    val baseContentPadding = 16.dp
}

/**
 * Элементы нижней навигации
 */
val bottomNavItems = listOf(
    BottomNavItem(
        route = Screen.Chat.route,
        title = "Чат",
        icon = "💬"
    ),
    BottomNavItem(
        route = Screen.Metrics.route,
        title = "Метрики",
        icon = "📊"
    ),
    BottomNavItem(
        route = Screen.Mcp.route,
        title = "MCP",
        icon = "🛠️"
    ),
    BottomNavItem(
        route = Screen.Settings.route,
        title = "Настройки",
        icon = "⚙️"
    )
)

/**
 * Элемент нижней навигации
 */
data class BottomNavItem(
    val route: String,
    val title: String,
    val icon: String
)

/**
 * Нижняя навигационная панель
 */
@Composable
private fun BottomNavigation(navController: NavController) {
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        bottomNavItems.forEach { item ->
            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true

            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Text(text = item.icon)
                },
                label = {
                    Text(text = item.title)
                }
            )
        }
    }
}

@Composable
fun App() {
    MaterialTheme {
        val navController = rememberNavController()

        Scaffold(
            bottomBar = {
                BottomNavigation(navController)
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues)
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

                    // Экран метрик
                    composable(Screen.Metrics.route) {
                        ChatMetricsScreen()
                    }

                    // Экран MCP (список инструментов)
                    composable(Screen.Mcp.route) {
                        McpScreen()
                    }

                    // Экран настроек
                    composable(Screen.Settings.route) {
                        SettingsScreen(
                            onNavigateBack = {
                                navController.navigate(Screen.Chat.route)
                            }
                        )
                    }
                }
            }
        }
    }
}
