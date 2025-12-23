package ru.izhxx.aichallenge.core.ui.navigation

/**
 * Интерфейс обработчика навигационных намерений.
 *
 * Назначение:
 * - Определяет контракт для обработки навигационных событий в приложении.
 * - Используется в паттерне Chain of Responsibility для передачи навигационных команд.
 * - Позволяет зарегистрировать множество обработчиков через [NavigationBus].
 *
 * Применение:
 * - Реализуйте этот интерфейс для каждого модуля/фичи, которая должна обрабатывать навигацию.
 * - Обработчик возвращает true, если намерение было обработано, false иначе.
 * - Если ни один обработчик не вернул true, можно выполнить fallback логику.
 *
 * Правила:
 * - Обработчик должен вернуть true только если он успешно обработал намерение.
 * - Обработчик должен игнорировать намерения, которые не относятся к его зоне ответственности.
 * - Избегайте побочных эффектов, если намерение не было обработано.
 *
 * Пример:
 * ```kotlin
 * class FeatureNavigationHandler : NavigationHandler {
 *     override fun handle(intent: NavigationIntent): Boolean {
 *         return when (intent) {
 *             is FeatureNavigationIntent.OpenDetails -> {
 *                 // Обработка навигации
 *                 navigator.navigateToDetails(intent.id)
 *                 true
 *             }
 *             else -> false // Не наша ответственность
 *         }
 *     }
 * }
 * ```
 *
 * @see NavigationIntent
 * @see NavigationBus
 */
interface NavigationHandler {

    /**
     * Обрабатывает навигационное намерение.
     *
     * @param intent Навигационное намерение для обработки.
     * @return true если намерение было успешно обработано, false иначе.
     */
    fun handle(intent: NavigationIntent): Boolean
}
