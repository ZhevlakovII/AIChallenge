package ru.izhxx.aichallenge.core.network.impl.clients.websocket

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments
import io.ktor.websocket.Frame
import io.ktor.websocket.readBytes
import io.ktor.websocket.readText
import io.ktor.websocket.readReason
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.yield
import ru.izhxx.aichallenge.core.network.api.config.NetworkConfig
import ru.izhxx.aichallenge.core.network.api.clients.websocket.WebSocketClient
import ru.izhxx.aichallenge.core.network.api.clients.websocket.WsEvent
import ru.izhxx.aichallenge.core.network.api.errors.NetworkErrorMapper

internal class WebSocketClientImpl(
    private val client: HttpClient,
    private val config: NetworkConfig,
    private val errorMapper: NetworkErrorMapper
) : WebSocketClient {

    override fun stream(
        path: String,
        headers: Map<String, String>
    ): Flow<WsEvent> = channelFlow {
        var attempt = 0
        while (isActive) {
            try {
                client.webSocket(
                    request = {
                        url { appendPath(path) }
                        headers.forEach { (k, v) -> this.headers.append(k, v) }
                    }
                ) {
                    trySend(WsEvent.Connected)
                    attempt = 0 // сбросить backoff после успешного подключения

                    for (frame in incoming) {
                        when (frame) {
                            is Frame.Text -> trySend(WsEvent.Text(frame.readText()))
                            is Frame.Binary -> trySend(WsEvent.Binary(frame.readBytes()))
                            is Frame.Close -> {
                                trySend(WsEvent.Closed(frame.readReason()?.code, frame.readReason()?.message))
                            }
                            is Frame.Ping, is Frame.Pong -> {
                                // игнорируем, пинг/понг handled плагином
                            }

                            else -> {}
                        }
                    }

                    // session завершился без исключений: считаем нормальным закрытием
                    // выход из webSocket блока
                }

                // Нормальное завершение — выходим из цикла без реконнекта
                break
            } catch (t: Throwable) {
                trySend(WsEvent.Failure(errorMapper.map(t, null)))
                val delayMs = config.ws.backoff.delayMillis(attempt++)
                if (delayMs <= 0) continue
                delay(delayMs)
            } finally {
                // кооперативно уступим планировщику
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
