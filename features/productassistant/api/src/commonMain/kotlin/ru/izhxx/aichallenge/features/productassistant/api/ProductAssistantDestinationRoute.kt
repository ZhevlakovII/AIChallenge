package ru.izhxx.aichallenge.features.productassistant.api

import ru.izhxx.aichallenge.core.ui.navigation.DestinationRoute

/**
 * Navigation route for Product Assistant feature
 */
object ProductAssistantDestinationRoute : DestinationRoute {
    const val ROUTE = "product_assistant"

    override fun invoke(): String = ROUTE
}
