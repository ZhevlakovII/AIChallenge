package ru.izhxx.aichallenge.core.network.impl.logger

import io.ktor.client.plugins.logging.Logger
import ru.izhxx.aichallenge.core.logger.Tag
import ru.izhxx.aichallenge.core.logger.debug
import ru.izhxx.aichallenge.core.network.impl.HttpClientCreatorImpl

/**
 * Реализация [Logger] для интеграции Ktor Logging с системой логирования приложения.
 *
 * Назначение:
 * - Перенаправляет логи Ktor HTTP клиента в централизованную систему логирования приложения.
 * - Позволяет видеть HTTP запросы и ответы в едином формате вместе с другими логами.
 * - Используется автоматически в [HttpClientCreatorImpl] при установке плагина Logging.
 *
 * Особенности:
 * - Логи выводятся на уровне DEBUG с тегом "KtorLoggerImpl".
 * - В debug сборках логирование включено (видны все HTTP запросы/ответы).
 * - В release сборках логирование отключено (LogLevel.NONE в HttpClientCreatorImpl).
 *
 * Правила:
 * - Эта реализация является internal и не должна использоваться напрямую.
 * - Автоматически используется в HttpClientCreatorImpl.
 *
 * @see Logger
 * @see HttpClientCreatorImpl
 * @see debug
 */
internal class KtorLoggerImpl : Logger {

    private val loggerTag = Tag.of("KtorLoggerImpl")

    override fun log(message: String) {
        debug(loggerTag) { message }
    }
}
