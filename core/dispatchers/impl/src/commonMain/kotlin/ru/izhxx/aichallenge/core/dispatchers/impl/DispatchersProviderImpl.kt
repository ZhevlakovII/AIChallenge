package ru.izhxx.aichallenge.core.dispatchers.impl

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import ru.izhxx.aichallenge.core.dispatchers.api.DispatchersProvider

/**
 * Реализация [DispatchersProvider] для предоставления диспетчеров корутин.
 *
 * Назначение:
 * - Предоставляет платформо-специфичные диспетчеры корутин через единый интерфейс.
 * - Используется в Koin DI для внедрения зависимостей в классы приложения.
 * - Реализует абстракцию [DispatchersProvider] для упрощения тестирования.
 *
 * Особенности реализации:
 * - [main] использует стандартный [Dispatchers.Main] для UI операций.
 * - [io] использует платформо-специфичный диспетчер через [provideIODispatcher].
 * - [default] использует стандартный [Dispatchers.Default] для CPU-bound задач.
 *
 * Платформо-специфичные детали:
 * - Android: IO диспетчер оптимизирован для файловых операций и сети.
 * - JVM/Desktop: IO диспетчер использует dedicated thread pool для блокирующих операций.
 * - iOS: IO диспетчер адаптирован под iOS threading model.
 *
 * Пример использования (через Koin DI):
 * ```kotlin
 * class MyRepository(private val dispatchers: DispatchersProvider) {
 *     suspend fun loadData() = withContext(dispatchers.io) { ... }
 * }
 * ```
 *
 * @see DispatchersProvider
 * @see provideIODispatcher
 */
internal class DispatchersProviderImpl : DispatchersProvider {
    override val main: CoroutineDispatcher = Dispatchers.Main
    override val io: CoroutineDispatcher = provideIODispatcher()
    override val default: CoroutineDispatcher = Dispatchers.Default
}
