package ru.izhxx.aichallenge.features.pranalyzer.api

import ru.izhxx.aichallenge.core.ui.navigation.DestinationRoute

/**
 * Навигационный контракт фичи PR Analyzer.
 * Используется приложением и другими модулями без зависимости от impl.
 */
object PrAnalyzerDestinationRoute : DestinationRoute {

    private const val ROUTE: String = "pranalyzer"

    override fun invoke(): String = ROUTE
}
