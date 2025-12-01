package ru.izhxx.aichallenge.core.network.core.api.response

/**
 * Непривязанное к реализации представление ответа для core-интерцепторов/логики.
 * Не содержит типов Ktor.
 *
 * Иммутабельный объект; изменения вносятся через [copy].
 */
data class ResponseContext(
    /** HTTP-статус код. */
    val statusCode: Int,
    /** Заголовки ответа. Ключи — без учёта регистра на уровне применения. */
    val headers: Map<String, String> = emptyMap(),
    /**
     * Снимок тела ответа (опционально).
     * Не гарантируется, что содержит полное тело (может быть обрезано на уровне impl).
     */
    val bodyBytes: ByteArray? = null,
    /**
     * Длительность выполнения запроса в миллисекундах (если доступно).
     */
    val durationMillis: Long? = null,
    /**
     * Момент окончания выполнения запроса (мс, unix time), может быть заполнен impl-слоем.
     */
    val endTimestampMillis: Long? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ResponseContext

        if (statusCode != other.statusCode) return false
        if (durationMillis != other.durationMillis) return false
        if (endTimestampMillis != other.endTimestampMillis) return false
        if (headers != other.headers) return false
        if (!bodyBytes.contentEquals(other.bodyBytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = statusCode
        result = 31 * result + (durationMillis?.hashCode() ?: 0)
        result = 31 * result + (endTimestampMillis?.hashCode() ?: 0)
        result = 31 * result + headers.hashCode()
        result = 31 * result + (bodyBytes?.contentHashCode() ?: 0)
        return result
    }
}
