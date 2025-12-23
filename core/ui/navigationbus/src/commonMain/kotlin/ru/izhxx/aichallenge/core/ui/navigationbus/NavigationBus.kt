package ru.izhxx.aichallenge.core.ui.navigationbus

import ru.izhxx.aichallenge.core.ui.navigation.NavigationHandler
import ru.izhxx.aichallenge.core.ui.navigation.NavigationIntent

/**
 * Центральная шина для маршрутизации навигационных намерений между модулями.
 *
 * Назначение:
 * - Реализует паттерн Event Bus для навигации в приложении.
 * - Позволяет модулям общаться через навигационные намерения без прямых зависимостей.
 * - Использует паттерн Chain of Responsibility для обработки навигационных команд.
 *
 * Принцип работы:
 * 1. Модули регистрируют свои [NavigationHandler] через [register].
 * 2. При вызове [send] шина последовательно передает намерение всем обработчикам.
 * 3. Первый обработчик, вернувший true, считается обработавшим намерение.
 * 4. Если ни один обработчик не вернул true, вызывается fallback (если указан).
 *
 * Правила использования:
 * - Регистрируйте обработчики при инициализации модуля/фичи.
 * - Отменяйте регистрацию ([unregister]) при удалении модуля.
 * - Используйте [clear] для очистки всех обработчиков (например, при логауте).
 * - Предоставляйте fallback для обработки неизвестных намерений.
 *
 * Потокобезопасность:
 * - Класс не является потокобезопасным. Используйте на главном потоке (UI thread).
 *
 * Пример:
 * ```kotlin
 * // В настройке приложения (через Koin):
 * val navigationBus = NavigationBus()
 *
 * // В модуле/фиче:
 * class ProfileModule(private val navigationBus: NavigationBus) {
 *     private val handler = ProfileNavigationHandler()
 *
 *     fun onStart() {
 *         navigationBus.register(handler)
 *     }
 *
 *     fun onStop() {
 *         navigationBus.unregister(handler)
 *     }
 * }
 *
 * // Использование:
 * navigationBus.send(
 *     intent = ProfileNavigationIntent.OpenProfile("user123"),
 *     fallback = { Log.w("Navigation", "Unhandled intent") }
 * )
 * ```
 *
 * @see NavigationHandler
 * @see NavigationIntent
 */
class NavigationBus {
    private val handlers = mutableListOf<NavigationHandler>()

    /**
     * Регистрирует обработчик навигации.
     *
     * @param handler Обработчик для регистрации.
     */
    fun register(handler: NavigationHandler) {
        handlers.add(handler)
    }

    /**
     * Отменяет регистрацию обработчика навигации.
     *
     * @param handler Обработчик для удаления.
     */
    fun unregister(handler: NavigationHandler) {
        handlers.remove(handler)
    }

    /**
     * Очищает все зарегистрированные обработчики.
     * Используйте для полного сброса навигации (например, при логауте).
     */
    fun clear() {
        handlers.clear()
    }

    /**
     * Отправляет навигационное намерение всем зарегистрированным обработчикам.
     *
     * Обработчики вызываются в порядке регистрации. Первый обработчик, вернувший true,
     * считается обработавшим намерение. Если ни один не вернул true, вызывается [fallback].
     *
     * @param T Тип навигационного намерения.
     * @param intent Навигационное намерение для отправки.
     * @param fallback Опциональная функция, вызываемая если намерение не было обработано.
     */
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
