package ru.izhxx.aichallenge.core.dispatchers.api

import kotlinx.coroutines.CoroutineDispatcher

/**
 * Интерфейс для централизованного предоставления диспетчеров для корутин (CoroutineDispatcher).
 *
 * Назначение:
 * - Абстракция над платформо-специфичными диспетчерами корутин.
 * - Упрощает тестирование (можно подменить на тестовые диспетчеры).
 * - Обеспечивает единую точку доступа к диспетчерам через Dependency Injection (Koin).
 *
 * Диспетчеры:
 * - [main] — для работы с UI (Android Main thread, iOS Main queue, Desktop UI thread).
 * - [io] — для блокирующих I/O операций (сеть, файлы, база данных).
 * - [default] — для CPU-интенсивных операций (обработка данных, вычисления).
 *
 * Правила использования:
 * - Внедряйте [DispatchersProvider] через конструктор в классы, использующие корутины.
 * - Используйте [main] только для обновления UI и кратких операций.
 * - [io] подходит для длительных блокирующих операций (файловая система, сеть).
 * - [default] используйте для CPU-bound задач (парсинг, сортировка, криптография).
 * - Избегайте прямого использования Dispatchers.Main/IO/Default — всегда используйте [DispatchersProvider].
 *
 * Пример:
 * ```kotlin
 * class MyRepository(private val dispatchers: DispatchersProvider) {
 *     suspend fun loadData(): Result<Data> = withContext(dispatchers.io) {
 *         // Блокирующая операция чтения из сети/БД
 *         api.fetchData()
 *     }
 * }
 * ```
 *
 * @see CoroutineDispatcher
 */
interface DispatchersProvider {
    /**
     * Диспетчер для работы с UI-потоком.
     * Используйте для обновления UI и коротких операций в главном потоке.
     */
    val main: CoroutineDispatcher

    /**
     * Диспетчер для блокирующих I/O операций.
     * Используйте для работы с сетью, файловой системой, базой данных.
     */
    val io: CoroutineDispatcher

    /**
     * Диспетчер для CPU-интенсивных операций.
     * Используйте для вычислений, обработки данных, парсинга.
     */
    val default: CoroutineDispatcher
}