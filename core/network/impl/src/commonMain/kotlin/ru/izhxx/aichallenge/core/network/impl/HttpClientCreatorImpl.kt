package ru.izhxx.aichallenge.core.network.impl

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import ru.izhxx.aichallenge.core.buildmode.isDebugBuild
import ru.izhxx.aichallenge.core.network.api.HttpClientCreator
import ru.izhxx.aichallenge.core.network.impl.logger.KtorLoggerImpl

/**
 * Реализация [HttpClientCreator] для создания Ktor HttpClient с автоматической конфигурацией.
 *
 * Назначение:
 * - Создает HttpClient с автоматически настроенным логированием в зависимости от режима сборки.
 * - Применяет кастомную конфигурацию через DSL блок, переданный в [buildHttpClient].
 * - Используется в Koin DI для внедрения в классы, требующие HTTP клиент.
 *
 * Особенности реализации:
 * - Автоматически устанавливает плагин Logging с [KtorLoggerImpl].
 * - В debug сборках логирование включено на уровне LogLevel.ALL (все запросы и ответы).
 * - В release сборках логирование отключено (LogLevel.NONE) для оптимизации производительности.
 * - Пользовательская конфигурация (из блока) применяется до установки логирования.
 *
 * Правила:
 * - Эта реализация является internal и не должна использоваться напрямую.
 * - Для создания HttpClient используйте [HttpClientCreator] через Koin DI.
 *
 * @see HttpClientCreator
 * @see KtorLoggerImpl
 * @see isDebugBuild
 */
internal class HttpClientCreatorImpl : HttpClientCreator {

    override fun buildHttpClient(block: HttpClientConfig<*>.() -> Unit): HttpClient {
        return HttpClient {
            block()
            install(Logging) {
                level = if (isDebugBuild()) LogLevel.ALL else LogLevel.NONE
                logger = KtorLoggerImpl()
            }
        }
    }
}
