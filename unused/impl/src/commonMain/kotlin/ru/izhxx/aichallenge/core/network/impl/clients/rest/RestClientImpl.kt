package ru.izhxx.aichallenge.core.network.impl.clients.rest

import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.utils.io.core.isEmpty
import io.ktor.utils.io.core.readAvailable
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import ru.izhxx.aichallenge.core.foundation.error.AppError
import ru.izhxx.aichallenge.core.foundation.result.AppResult as AppResult
import ru.izhxx.aichallenge.core.network.api.clients.rest.RestClient
import ru.izhxx.aichallenge.core.network.api.config.NetworkConfig
import ru.izhxx.aichallenge.core.network.api.errors.NetworkErrorMapper
import ru.izhxx.aichallenge.core.network.impl.clients.factory.appendQueryParams
import ru.izhxx.aichallenge.core.network.impl.errors.HttpStatusError

internal class RestClientImpl(
    private val client: HttpClient,
    private val config: NetworkConfig,
    private val errorMapper: NetworkErrorMapper
) : RestClient {

    private val json: Json = config.json.toJson()

    override suspend fun <T : Any> get(
        path: String,
        query: Map<String, Any?>,
        headers: Map<String, String>,
        deserializer: DeserializationStrategy<T>
    ): AppResult<T> {
        return try {
            val response = client.get {
                url {
                    appendPath(path)
                    appendQueryParams(query)
                }
                headers.forEach { (k, v) -> header(k, v) }
            }
            ensureSuccessOrReturnFailure(response)?.let { return it }
            val text = response.bodyAsText()
            val value = json.decodeFromString(deserializer, text)
            AppResult.success(value)
        } catch (t: Throwable) {
            AppResult.failure(errorMapper.map(t, null))
        }
    }

    override suspend fun <I : Any, O : Any> post(
        path: String,
        body: I,
        headers: Map<String, String>,
        deserializer: DeserializationStrategy<O>
    ): AppResult<O, AppError> {
        return try {
            val response = client.post {
                url { appendPath(path) }
                contentType(ContentType.Application.Json)
                headers.forEach { (k, v) -> header(k, v) }
                setBody(body)
            }
            ensureSuccessOrReturnFailure(response)?.let { return it }
            val text = response.bodyAsText()
            val value = json.decodeFromString(deserializer, text)
            AppResult.success(value)
        } catch (t: Throwable) {
            AppResult.failure(errorMapper.map(t, null))
        }
    }

    override suspend fun <I : Any, O : Any> put(
        path: String,
        body: I,
        headers: Map<String, String>,
        deserializer: DeserializationStrategy<O>
    ): AppResult<O> {
        return try {
            val response = client.put {
                url { appendPath(path) }
                contentType(ContentType.Application.Json)
                headers.forEach { (k, v) -> header(k, v) }
                setBody(body)
            }
            ensureSuccessOrReturnFailure(response)?.let { return it }
            val text = response.bodyAsText()
            val value = json.decodeFromString(deserializer, text)
            AppResult.success(value)
        } catch (t: Throwable) {
            AppResult.failure(errorMapper.map(t, null))
        }
    }

    override suspend fun <I : Any, O : Any> patch(
        path: String,
        body: I,
        headers: Map<String, String>,
        deserializer: DeserializationStrategy<O>
    ): AppResult<O, AppError> {
        return try {
            val response = client.patch {
                url { appendPath(path) }
                contentType(ContentType.Application.Json)
                headers.forEach { (k, v) -> header(k, v) }
                setBody(body)
            }
            ensureSuccessOrReturnFailure(response)?.let { return it }
            val text = response.bodyAsText()
            val value = json.decodeFromString(deserializer, text)
            AppResult.success(value)
        } catch (t: Throwable) {
            AppResult.failure(errorMapper.map(t, null))
        }
    }

    override suspend fun delete(
        path: String,
        headers: Map<String, String>
    ): AppResult<Unit, AppError> {
        return try {
            val response = client.delete {
                url { appendPath(path) }
                headers.forEach { (k, v) -> header(k, v) }
            }
            ensureSuccessOrReturnFailure(response)?.let { return it }
            AppResult.success(Unit)
        } catch (t: Throwable) {
            AppResult.failure(errorMapper.map(t, null))
        }
    }

    override suspend fun getBytes(
        path: String,
        query: Map<String, Any?>,
        headers: Map<String, String>,
        bufferSize: Int
    ): Flow<AppResult<ByteArray, AppError>> = flow {
        try {
            val response = client.get {
                url {
                    appendPath(path)
                    appendQueryParams(query)
                }
                headers.forEach { (k, v) -> header(k, v) }
                header(HttpHeaders.Accept, "*/*")
            }
            ensureSuccessOrReturnFailure(response)?.let {
                emit(it)
                return@flow
            }
            val channel = response.bodyAsChannel()
            val buf = ByteArray(bufferSize)
            while (!channel.isClosedForRead) {
                val packet = channel.readRemaining()
                while (!packet.isEmpty) {
                    val read = packet.readAvailable(buf, 0, bufferSize)
                    if (read > 0) {
                        emit(AppResult.success(buf.copyOf(read)))
                    } else {
                        break
                    }
                }
            }
        } catch (t: Throwable) {
            emit(AppResult.failure(errorMapper.map(t, null)))
        }
    }

    override suspend fun getLines(
        path: String,
        query: Map<String, Any?>,
        headers: Map<String, String>,
        charset: io.ktor.utils.io.charsets.Charset
    ): Flow<AppResult<String, AppError>> = flow {
        try {
            val response = client.get {
                url {
                    appendPath(path)
                    appendQueryParams(query)
                }
                headers.forEach { (k, v) -> header(k, v) }
                header(HttpHeaders.Accept, "*/*")
            }
            ensureSuccessOrReturnFailure(response)?.let {
                emit(it)
                return@flow
            }

            val delimiter = '\n'
            val channel = response.bodyAsChannel()
            val buffer = ByteArray(8192)
            val sb = StringBuilder()
            while (!channel.isClosedForRead) {
                val packet = channel.readRemaining()
                while (!packet.isEmpty) {
                    val read = packet.readAvailable(buffer, 0, buffer.size)
                    if (read <= 0) break
                    // TODO: корректная декодировка по заданной charset.
                    // Сейчас используем decodeToString() (UTF-8 по-дефолту на платформах).
                    sb.append(buffer.decodeToString(0, read))
                    var idx = sb.indexOf(delimiter)
                    while (idx != -1) {
                        val line = sb.substring(0, idx).trimEnd('\r')
                        emit(AppResult.success(line))
                        sb.deleteRange(0, idx + 1)
                        idx = sb.indexOf(delimiter)
                    }
                    // защита от роста буфера без разделителя
                    if (sb.length > config.streaming.maxLineBytes) {
                        emit(
                            AppResult.failure(
                                AppError.SerializationError(
                                    code = "stream.line_too_long",
                                    rawMessage = "Line exceeds maxLineBytes=${config.streaming.maxLineBytes}"
                                )
                            )
                        )
                        return@flow
                    }
                }
            }
            if (sb.isNotEmpty()) {
                emit(AppResult.success(sb.toString()))
            }
        } catch (t: Throwable) {
            emit(AppResult.failure(errorMapper.map(t, null)))
        }
    }

    override suspend fun <T : Any> getNdjson(
        path: String,
        query: Map<String, Any?>,
        headers: Map<String, String>,
        deserializer: DeserializationStrategy<T>
    ): Flow<AppResult<T, AppError>> = flow {
        try {
            val response = client.get {
                url {
                    appendPath(path)
                    appendQueryParams(query)
                }
                headers.forEach { (k, v) -> header(k, v) }
                header(HttpHeaders.Accept, "application/x-ndjson,application/json;q=0.9,*/*;q=0.8")
            }
            ensureSuccessOrReturnFailure(response)?.let {
                emit(it)
                return@flow
            }
            val channel = response.bodyAsChannel()
            val sb = StringBuilder()
            val buffer = ByteArray(8192)
            while (!channel.isClosedForRead) {
                val packet = channel.readRemaining()
                while (!packet.isEmpty) {
                    val read = packet.readAvailable(buffer, 0, buffer.size)
                    if (read <= 0) break
                    sb.append(buffer.decodeToString(0, read))
                    var idx = sb.indexOf('\n')
                    while (idx != -1) {
                        val line = sb.substring(0, idx).trim()
                        if (line.isNotEmpty()) {
                            try {
                                emit(AppResult.success(json.decodeFromString(deserializer, line)))
                            } catch (t: Throwable) {
                                emit(AppResult.failure(errorMapper.map(t, null)))
                                return@flow
                            }
                        }
                        sb.deleteRange(0, idx + 1)
                        idx = sb.indexOf('\n')
                    }
                    if (sb.length > config.streaming.maxLineBytes) {
                        emit(
                            AppResult.failure(
                                AppError.SerializationError(
                                    code = "stream.line_too_long",
                                    rawMessage = "NDJSON line exceeds maxLineBytes=${config.streaming.maxLineBytes}"
                                )
                            )
                        )
                        return@flow
                    }
                }
            }
            val tail = sb.toString().trim()
            if (tail.isNotEmpty()) {
                try {
                    emit(AppResult.success(json.decodeFromString(deserializer, tail)))
                } catch (t: Throwable) {
                    emit(AppResult.failure(errorMapper.map(t, null)))
                }
            }
        } catch (t: Throwable) {
            emit(AppResult.failure(errorMapper.map(t, null)))
        }
    }

    /**
     * Если ответ неуспешный — возвращает Failure(AppError) для немедленной отдачи наружу.
     * Иначе возвращает null.
     */
    private suspend fun <T> ensureSuccessOrReturnFailure(response: HttpResponse): AppResult<T>? {
        if (!response.status.isSuccess()) {
            val text = runCatching { response.bodyAsText() }.getOrNull()
            val error = errorMapper.map(HttpStatusError(response.status, text), response)
            return AppResult.failure(error)
        }
        return null
    }
}

/**
 * Локальный хелпер для относительного пути.
 */
private fun URLBuilder.appendPath(path: String) {
    val p = path.trim('/')
    if (p.isEmpty()) return
    appendPathSegments(*p.split('/').toTypedArray())
}
