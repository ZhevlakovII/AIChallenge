package ru.izhxx.aichallenge.core.network.impl.clients.sse

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments
import io.ktor.http.isSuccess
import io.ktor.utils.io.core.readAvailable
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.yield
import ru.izhxx.aichallenge.core.network.api.clients.sse.SseClient
import ru.izhxx.aichallenge.core.network.api.clients.sse.SseEvent
import ru.izhxx.aichallenge.core.network.api.config.NetworkConfig

internal class SseClientImpl(
    private val client: HttpClient,
    private val config: NetworkConfig,
    private val errorMapper: ru.izhxx.aichallenge.core.network.api.errors.NetworkErrorMapper
) : SseClient {

    override fun open(
        path: String,
        headers: Map<String, String>,
        lastEventId: String?
    ): Flow<SseEvent> = channelFlow {
        var attempt = 0
        var nextRetryMs: Long? = null
        var lastId: String? = lastEventId

        while (isActive) {
            try {
                val response = client.get {
                    url { appendPath(path) }
                    header(HttpHeaders.Accept, "text/event-stream")
                    if (!lastId.isNullOrEmpty()) {
                        header("Last-Event-ID", lastId)
                    }
                    headers.forEach { (k, v) -> header(k, v) }
                }

                // Проверка статуса и content-type
                if (!response.status.isSuccess()) {
                    val text = runCatching { response.bodyAsText() }.getOrNull()
                    throw ru.izhxx.aichallenge.core.network.impl.errors.HttpStatusError(response.status, text)
                }
                val ct = response.headers[HttpHeaders.ContentType] ?: ""
                if (!ct.lowercase().startsWith("text/event-stream")) {
                    throw IllegalStateException("Unexpected content-type for SSE: '$ct'")
                }

                val channel = response.bodyAsChannel()
                val sb = StringBuilder()

                // Текущий блок
                var curId: String? = null
                var curEvent: String? = null
                val curData = StringBuilder()

                suspend fun emitBlockIfAny() {
                    if (curId == null && curEvent == null && curData.isEmpty()) {
                        // пустой блок — ничего
                        return
                    }
                    val msg = SseEvent.Message(
                        id = curId,
                        event = curEvent,
                        data = curData.toString()
                    )
                    trySend(msg)
                    // обновим lastId для реконнекта
                    if (!curId.isNullOrEmpty()) {
                        lastId = curId
                    }
                    // сбросим блок
                    curId = null
                    curEvent = null
                    curData.clear()
                }

                // Считываем поток построчно
                val buf = ByteArray(8192)
                while (!channel.isClosedForRead && isActive) {
                    val packet = channel.readRemaining()
                    while (!packet.exhausted()) {
                        val read = packet.readAvailable(buf, 0, buf.size)
                        if (read <= 0) break
                        sb.append(buf.decodeToString(0, read))

                        var idx = sb.indexOf('\n')
                        while (idx != -1) {
                            val line = sb.substring(0, idx).trimEnd('\r')
                            sb.deleteRange(0, idx + 1)
                            // Пустая строка — конец блока
                            if (line.isEmpty()) {
                                emitBlockIfAny()
                            } else if (line.startsWith(":")) {
                                // Комментарий
                                trySend(SseEvent.Comment(line.drop(1)))
                            } else {
                                val colon = line.indexOf(':')
                                val field = if (colon == -1) line else line.substring(0, colon)
                                val rawValue = if (colon == -1) "" else line.substring(colon + 1)
                                val value = if (rawValue.startsWith(" ")) rawValue.drop(1) else rawValue
                                when (field) {
                                    "data" -> {
                                        if (curData.isNotEmpty()) curData.append('\n')
                                        curData.append(value)
                                    }
                                    "event" -> curEvent = value
                                    "id" -> curId = value
                                    "retry" -> {
                                        val v = value.toLongOrNull()
                                        if (v != null && v >= 0) {
                                            nextRetryMs = v
                                            trySend(SseEvent.RetryMillis(v))
                                        }
                                    }
                                }
                            }
                            idx = sb.indexOf('\n')
                        }
                    }
                }

                // Хвост (если нет пустой строки в конце)
                if (sb.isNotEmpty()) {
                    val tail = sb.toString()
                    // Если финальная строка не пуста — обработаем
                    if (tail.isNotEmpty()) {
                        // Может быть часть последней строки без \n — добавим как data
                        if (tail.isNotBlank()) {
                            if (tail.startsWith(":")) {
                                trySend(SseEvent.Comment(tail.drop(1)))
                            } else {
                                val colon = tail.indexOf(':')
                                val field = if (colon == -1) tail else tail.substring(0, colon)
                                val rawValue = if (colon == -1) "" else tail.substring(colon + 1)
                                val value = if (rawValue.startsWith(" ")) rawValue.drop(1) else rawValue
                                when (field) {
                                    "data" -> {
                                        if (curData.isNotEmpty()) curData.append('\n')
                                        curData.append(value)
                                    }
                                    "event" -> curEvent = value
                                    "id" -> curId = value
                                    "retry" -> {
                                        val v = value.toLongOrNull()
                                        if (v != null && v >= 0) {
                                            nextRetryMs = v
                                            trySend(SseEvent.RetryMillis(v))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    sb.clear()
                }
                // Завершим последний блок, если был
                if (curId != null || curEvent != null || curData.isNotEmpty()) {
                    trySend(SseEvent.Message(id = curId, event = curEvent, data = curData.toString()))
                }

                // Нормальное завершение потока — сообщим и выйдем
                trySend(SseEvent.End)
                break
            } catch (t: Throwable) {
                trySend(SseEvent.Failure(errorMapper.map(t, null)))
                val delayMs = nextRetryMs ?: config.sse.backoff.delayMillis(attempt)
                attempt++
                // сбрасываем server-provided retry после применения
                nextRetryMs = null
                if (delayMs > 0) delay(delayMs)
            } finally {
                yield()
            }
        }
    }
}

private fun URLBuilder.appendPath(path: String) {
    val p = path.trim('/')
    if (p.isEmpty()) return
    appendPathSegments(*p.split('/').toTypedArray())
}
