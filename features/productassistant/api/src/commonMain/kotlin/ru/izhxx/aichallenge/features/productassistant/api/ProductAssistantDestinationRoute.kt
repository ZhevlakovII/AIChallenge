package ru.izhxx.aichallenge.features.productassistant.api

import ru.izhxx.aichallenge.core.ui.navigation.NavigationIntent

/**
 * Navigation route for Product Assistant feature
 */
object ProductAssistantDestinationRoute : NavigationIntent {
    const val ROUTE = "product_assistant"

    fun invoke(): String = ROUTE
}
