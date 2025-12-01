package ru.izhxx.aichallenge.core.network.impl.errors

import io.ktor.http.HttpStatusCode
import ru.izhxx.aichallenge.core.foundation.strings.appendSpace

/**
 * Исключение для статусов HTTP != 2xx, содержащее код и тело ответа (если доступно).
 */
internal class HttpStatusError(
    val status: HttpStatusCode,
    val bodyText: String?
) : Exception(
    message = StringBuilder().apply {
        append("HTTP")
        appendSpace()
        append(status.value)
        appendSpace()
        append(status.description)
        bodyText?.let {
            append("BodyText:")
            appendSpace()
            append("\"")
            append(it)
            append("\"")
        }
    }.toString()
)