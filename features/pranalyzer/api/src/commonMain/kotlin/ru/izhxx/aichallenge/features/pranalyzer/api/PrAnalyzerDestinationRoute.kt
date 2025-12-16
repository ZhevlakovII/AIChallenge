package ru.izhxx.aichallenge.features.pranalyzer.api

import ru.izhxx.aichallenge.core.ui.navigation.NavigationIntent

/**
 * Навигационный контракт фичи PR Analyzer.
 * Используется приложением и другими модулями без зависимости от impl.
 */
object PrAnalyzerDestinationRoute : NavigationIntent {

    private const val ROUTE: String = "pranalyzer"

    fun invoke(): String = ROUTE
}
