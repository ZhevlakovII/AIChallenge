package ru.izhxx.aichallenge.core.dispatchers.impl.di

import org.koin.dsl.module
import ru.izhxx.aichallenge.core.dispatchers.api.DispatchersProvider
import ru.izhxx.aichallenge.core.dispatchers.impl.DispatchersProviderImpl

/**
 * Модуль Koin для предоставления корутинных диспетчеров.
 *
 * Назначение:
 * - Регистрирует singleton реализацию [DispatchersProvider] в DI-контейнере.
 * - Позволяет внедрять диспетчеры в любой класс через конструктор.
 *
 * Регистрация:
 * - [DispatchersProvider] регистрируется как singleton, что гарантирует
 *   использование одних и тех же экземпляров диспетчеров во всем приложении.
 *
 * Использование:
 * ```kotlin
 * // В настройке Koin:
 * startKoin {
 *     modules(dispatchersModule, ...)
 * }
 *
 * // В классе (автоматическое внедрение):
 * class MyRepository(private val dispatchers: DispatchersProvider) {
 *     suspend fun doWork() = withContext(dispatchers.io) { ... }
 * }
 * ```
 *
 * @see DispatchersProvider
 * @see DispatchersProviderImpl
 */
val dispatchersModule = module {
    single<DispatchersProvider> { DispatchersProviderImpl() }
}
