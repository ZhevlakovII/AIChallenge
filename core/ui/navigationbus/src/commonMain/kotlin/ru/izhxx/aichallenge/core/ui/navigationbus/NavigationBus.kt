package ru.izhxx.aichallenge.core.ui.navigationbus

import ru.izhxx.aichallenge.core.ui.navigation.NavigationHandler
import ru.izhxx.aichallenge.core.ui.navigation.NavigationIntent

class NavigationBus {
    private val handlers = mutableListOf<NavigationHandler>()

    fun register(handler: NavigationHandler) {
        handlers.add(handler)
    }

    fun unregister(handler: NavigationHandler) {
        handlers.remove(handler)
    }

    fun clear() {
        handlers.clear()
    }

    fun <T : NavigationIntent> send(
        intent: T,
        fallback: (() -> Unit)? = null
    ) {
        val handled = handlers.any { it.handle(intent) }
        if (!handled) {
            fallback?.invoke()
        }
    }
}
