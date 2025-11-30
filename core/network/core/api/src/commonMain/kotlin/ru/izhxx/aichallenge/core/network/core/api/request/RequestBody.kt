package ru.izhxx.aichallenge.core.network.core.api.request

/**
 * Абстракция тела HTTP‑запроса без зависимости от Ktor.
 * Конкретное применение/кодирование выполняется в impl-слое.
 */
sealed interface RequestBody {
    /** MIME‑тип содержимого, например "application/json; charset=utf-8". */
    val contentType: String?

    data class Bytes(
        val bytes: ByteArray,
        override val contentType: String? = null
    ) : RequestBody {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Bytes

            if (!bytes.contentEquals(other.bytes)) return false
            if (contentType != other.contentType) return false

            return true
        }

        override fun hashCode(): Int {
            var result = bytes.contentHashCode()
            result = 31 * result + (contentType?.hashCode() ?: 0)
            return result
        }
    }

    data class Text(
        val text: String,
        override val contentType: String? = "text/plain; charset=utf-8"
    ) : RequestBody

    data class Json(
        val jsonString: String,
        override val contentType: String = "application/json; charset=utf-8"
    ) : RequestBody
}
