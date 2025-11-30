package ru.izhxx.aichallenge.core.network.impl.clients.factory

import io.ktor.client.HttpClient
import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments
import ru.izhxx.aichallenge.core.network.api.config.NetworkConfig

/**
 * Утилиты запроса/URL для всего модуля (package-level).
 */
internal fun URLBuilder.appendQueryParams(query: Map<String, Any?>) {
    query.forEach { (k, v) ->
        when (v) {
            null -> Unit
            is Iterable<*> -> v.forEach { vi -> if (vi != null) parameters.append(k, vi.toString()) }
            is Array<*> -> v.forEach { vi -> if (vi != null) parameters.append(k, vi.toString()) }
            else -> parameters.append(k, v.toString())
        }
    }
}

/**
 * Хелпер добавления относительного пути.
 */
internal fun URLBuilder.appendPath(path: String) {
    val p = path.trim('/')
    if (p.isEmpty()) return
    appendPathSegments(*p.split('/').toTypedArray())
}

/**
 * Ожидается на платформах (android/jvm/ios).
 */
internal expect fun createPlatformClient(config: NetworkConfig): HttpClient
