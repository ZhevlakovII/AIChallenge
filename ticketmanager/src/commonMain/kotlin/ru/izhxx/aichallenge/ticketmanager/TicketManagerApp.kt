package ru.izhxx.aichallenge.ticketmanager

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.savedstate.read
import ru.izhxx.aichallenge.ticketmanager.presentation.assistant.LLMAssistantScreen
import ru.izhxx.aichallenge.ticketmanager.presentation.details.TicketDetailsScreen
import ru.izhxx.aichallenge.ticketmanager.presentation.list.TicketListScreen
import ru.izhxx.aichallenge.ticketmanager.presentation.login.LoginScreen

/**
 * Главные экраны приложения
 */
sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object TicketList : Screen("ticketList")
    data object TicketDetails : Screen("ticketDetails/{ticketId}") {
        fun createRoute(ticketId: String) = "ticketDetails/$ticketId"
    }
    data object LLMAssistant : Screen("llmAssistant")
}

/**
 * Главный компонент приложения Ticket Manager
 */
@Composable
fun TicketManagerApp() {
    MaterialTheme {
        val navController = rememberNavController()

        Scaffold(
            modifier = Modifier.fillMaxSize()
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = Screen.Login.route,
                modifier = Modifier.padding(paddingValues)
            ) {
                // Экран входа
                composable(Screen.Login.route) {
                    LoginScreen(
                        onLoginSuccess = {
                            navController.navigate(Screen.TicketList.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        }
                    )
                }

                // Экран списка тикетов
                composable(Screen.TicketList.route) {
                    TicketListScreen(
                        onTicketClick = { ticketId ->
                            navController.navigate(Screen.TicketDetails.createRoute(ticketId))
                        },
                        onLLMAssistantClick = {
                            navController.navigate(Screen.LLMAssistant.route)
                        }
                    )
                }

                // Экран деталей тикета
                composable(
                    route = Screen.TicketDetails.route,
                    arguments = listOf(
                        navArgument("ticketId") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val ticketId = backStackEntry.arguments?.read {
                        getString("ticketId")
                    } ?: return@composable
                    TicketDetailsScreen(
                        ticketId = ticketId,
                        onBackClick = { navController.popBackStack() }
                    )
                }

                // Экран LLM ассистента
                composable(Screen.LLMAssistant.route) {
                    LLMAssistantScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
