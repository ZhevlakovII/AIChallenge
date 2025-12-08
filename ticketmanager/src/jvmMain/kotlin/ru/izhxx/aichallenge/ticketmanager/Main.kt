package ru.izhxx.aichallenge.ticketmanager

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.koin.core.context.startKoin
import ru.izhxx.aichallenge.ticketmanager.di.ticketManagerModule

fun main() {
    // Инициализация Koin
    startKoin {
        modules(ticketManagerModule)
    }

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Ticket Manager",
        ) {
            TicketManagerApp()
        }
    }
}
