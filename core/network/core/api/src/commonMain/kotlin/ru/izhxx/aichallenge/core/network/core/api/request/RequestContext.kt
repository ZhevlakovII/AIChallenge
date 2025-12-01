package ru.izhxx.aichallenge.core.network.core.api.request

/**
 * Непривязанное к реализации представление запроса для core-интерцепторов/логики.
 * Не содержит типов Ktor.
 *
 * Иммутабельный объект; изменения вносятся через [copy].
 */
data class RequestContext(
    /** Базовый URL (например, https://api.example.com). */
    val baseUrl: String,
    /** Относительный путь (например, /v1/users). */
    val path: String,
    /** HTTP-метод. */
    val method: HttpMethod,
    /**
     * Заголовки запроса. Ключи — без учёта регистра на уровне применения.
     * Здесь храним как есть, редактирование/маскирование — на уровне лог-политик.
     */
    val headers: Map<String, String> = emptyMap(),
    /**
     * Query-параметры. Значение может быть null (флаг-параметры без значения).
     */
    val query: Map<String, String?> = emptyMap(),
    /**
     * Тело запроса (опционально).
     */
    val body: RequestBody? = null,
    /**
     * Момент начала исполнения запроса (мс, unix time), может быть заполнен impl-слоем.
     */
    val startTimestampMillis: Long? = null
)
