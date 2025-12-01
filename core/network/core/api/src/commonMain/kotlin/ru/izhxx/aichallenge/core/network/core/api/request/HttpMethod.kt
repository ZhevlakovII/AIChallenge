package ru.izhxx.aichallenge.core.network.core.api.request

/**
 * HTTP-метод без привязки к конкретной реализации клиента.
 * Не утягивает типы Ktor в public API core.
 */
enum class HttpMethod {
    GET,
    POST,
    PUT,
    PATCH,
    DELETE,
    HEAD,
    OPTIONS;
}