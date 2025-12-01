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

    /**
     * Multipart form data (for file uploads).
     *
     * @param parts List of multipart parts
     * @param boundary Multipart boundary (auto-generated if null)
     */
    data class Multipart(
        val parts: List<Part>,
        val boundary: String? = null
    ) : RequestBody {
        override val contentType: String
            get() = "multipart/form-data; boundary=${boundary ?: "----KotlinMultipartBoundary"}"

        /**
         * A single part in multipart request.
         */
        sealed interface Part {
            val name: String

            data class FormField(
                override val name: String,
                val value: String
            ) : Part

            data class FileData(
                override val name: String,
                val filename: String,
                val contentType: String,
                val bytes: ByteArray
            ) : Part {
                override fun equals(other: Any?): Boolean {
                    if (this === other) return true
                    if (other == null || this::class != other::class) return false
                    other as FileData
                    if (name != other.name) return false
                    if (filename != other.filename) return false
                    if (contentType != other.contentType) return false
                    if (!bytes.contentEquals(other.bytes)) return false
                    return true
                }

                override fun hashCode(): Int {
                    var result = name.hashCode()
                    result = 31 * result + filename.hashCode()
                    result = 31 * result + contentType.hashCode()
                    result = 31 * result + bytes.contentHashCode()
                    return result
                }
            }
        }
    }

    /**
     * Streaming body for large files (avoids loading entire content into memory).
     *
     * Platform-specific implementation required.
     */
    // TODO: Platform-specific channel/stream abstraction
    data class Stream(
        val contentLength: Long?,
        override val contentType: String?,
        val provider: suspend () -> ByteArray
    ) : RequestBody
}
