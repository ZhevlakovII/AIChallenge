package ru.izhxx.aichallenge.core.network.api.errors

/**
 * Унифицированные сетевые ошибки.
 */
sealed interface NetworkError {
    data class Http(val code: Int, val message: String? = null, val body: String? = null) : NetworkError
    data object Network : NetworkError
    data object Timeout : NetworkError
    data object Serialization : NetworkError
    data object Cancelled : NetworkError
    data class Unknown(val cause: Throwable) : NetworkError
}
