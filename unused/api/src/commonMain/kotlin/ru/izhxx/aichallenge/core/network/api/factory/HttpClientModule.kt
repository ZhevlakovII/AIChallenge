package ru.izhxx.aichallenge.core.network.api.factory

import io.ktor.client.HttpClientConfig

/**
 * Модуль конфигурации HttpClient (устанавливает плагины/интерсепторы на builder).
 * Небольшие и независимые модули проще тестировать и переиспользовать.
 */
fun interface HttpClientModule {
    fun install(builder: HttpClientConfig<*>)
}
