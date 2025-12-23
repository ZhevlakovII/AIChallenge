package ru.izhxx.aichallenge.core.dispatchers.impl

import kotlinx.coroutines.CoroutineDispatcher
import ru.izhxx.aichallenge.core.dispatchers.api.DispatchersProvider

/**
 * Предоставляет платформо-специфичный IO диспетчер для блокирующих операций.
 *
 * Назначение:
 * - Абстракция для получения IO диспетчера, оптимизированного под конкретную платформу.
 * - Используется в [DispatchersProviderImpl] для предоставления IO диспетчера.
 *
 * Платформо-специфичные реализации:
 * - Android: возвращает Dispatchers.IO (оптимизирован для Android runtime).
 * - JVM/Desktop: возвращает Dispatchers.IO (с настройкой thread pool под Desktop).
 * - iOS: возвращает адаптированный диспетчер под iOS threading model.
 *
 * Правила:
 * - Эта функция является internal и не должна использоваться напрямую.
 * - Для получения IO диспетчера используйте [DispatchersProvider.io].
 *
 * @return CoroutineDispatcher для блокирующих I/O операций.
 * @see DispatchersProvider
 */
internal expect fun provideIODispatcher(): CoroutineDispatcher
