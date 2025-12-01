package ru.izhxx.aichallenge.core.network.api.clients.rest

import io.ktor.utils.io.charsets.Charset
import io.ktor.utils.io.charsets.Charsets
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.DeserializationStrategy
import ru.izhxx.aichallenge.core.foundation.error.AppError
import ru.izhxx.aichallenge.core.foundation.result.AppResult

/**
 * REST-клиент с поддержкой синхронных запросов и потоковых ответов.
 * Для десериализации используются явные сериализаторы (Strategy), чтобы не требовать inline reified в интерфейсе.
 * Для удобства можно предоставить reified-расширения на уровне вызова (в потребителях).
 *
 * Возвращает [AppResult] с [AppError].
 */
interface RestClient {
    // CRUD c десериализацией
    suspend fun <T : Any> get(
        path: String,
        query: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
        deserializer: DeserializationStrategy<T>
    ): AppResult<T>

    suspend fun <I : Any, O : Any> post(
        path: String,
        body: I,
        headers: Map<String, String> = emptyMap(),
        deserializer: DeserializationStrategy<O>
    ): AppResult<O>

    suspend fun <I : Any, O : Any> put(
        path: String,
        body: I,
        headers: Map<String, String> = emptyMap(),
        deserializer: DeserializationStrategy<O>
    ): AppResult<O>

    suspend fun <I : Any, O : Any> patch(
        path: String,
        body: I,
        headers: Map<String, String> = emptyMap(),
        deserializer: DeserializationStrategy<O>
    ): AppResult<O>

    suspend fun delete(
        path: String,
        headers: Map<String, String> = emptyMap()
    ): AppResult<Unit>

    // Потоковые режимы
    suspend fun getBytes(
        path: String,
        query: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
        bufferSize: Int = DEFAULT_BUFFER_SIZE
    ): Flow<AppResult<ByteArray>>

    suspend fun getLines(
        path: String,
        query: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
        charset: Charset = Charsets.UTF_8
    ): Flow<AppResult<String>>

    suspend fun <T : Any> getNdjson(
        path: String,
        query: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
        deserializer: DeserializationStrategy<T>
    ): Flow<AppResult<T>>

    companion object {
        const val DEFAULT_BUFFER_SIZE: Int = 8 * 1024
    }
}
