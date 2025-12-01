package ru.izhxx.aichallenge.core.network.core.api.serialization

/**
 * Supported content formats for request/response serialization.
 */
enum class ContentFormat(val mimeType: String) {

    /**
     * JSON serialization (kotlinx.serialization).
     */
    JSON("application/json"),

    /**
     * ProtoBuf serialization (kotlinx.serialization protobuf).
     */
    PROTOBUF("application/protobuf"),

    /**
     * Plain text (no serialization).
     */
    TEXT("text/plain"),

    /**
     * Binary data (raw bytes).
     */
    BINARY("application/octet-stream"),

    /**
     * HTML content.
     */
    HTML("text/html");

    companion object {

        /**
         * Parse ContentFormat from MIME type string.
         */
        fun fromMimeType(mimeType: String): ContentFormat? {
            val normalized = mimeType.substringBefore(';').trim().lowercase()
            return entries.find { it.mimeType.equals(normalized, ignoreCase = true) }
        }
    }
}
