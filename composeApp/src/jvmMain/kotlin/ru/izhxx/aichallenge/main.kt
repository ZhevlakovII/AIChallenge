package ru.izhxx.aichallenge

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.koin.core.context.startKoin
import ru.izhxx.aichallenge.di.appModule

fun main() {
    // Инициализация Koin для Desktop
    startKoin {
        // Загружаем модули: appModule из composeApp и модули из shared
        modules(appModule)
    }
    
    application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "AIChallenge",
    ) {
        App()
    }
}}
