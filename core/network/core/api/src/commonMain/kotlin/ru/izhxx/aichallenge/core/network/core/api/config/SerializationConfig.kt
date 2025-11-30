package ru.izhxx.aichallenge.core.network.core.api.config

/**
 * Настройки сериализации данных.
 * Не содержит конкретных зависимостей на kotlinx.serialization.Json,
 * чтобы не протаскивать реализацию в public API core.
 */
data class SerializationConfig(
    val ignoreUnknownKeys: Boolean = true,
    val isLenient: Boolean = false,
    val encodeDefaults: Boolean = true,
    val allowStructuredMapKeys: Boolean = false,
    val prettyPrint: Boolean = false,
    val explicitNulls: Boolean = true,
    val coerceInputValues: Boolean = true
) {
    companion object {
        /** Строгие, но практичные дефолты для большинства prod-кейсов. */
        val DefaultStrict = SerializationConfig(
            ignoreUnknownKeys = true,
            isLenient = false,
            encodeDefaults = true,
            allowStructuredMapKeys = false,
            prettyPrint = false,
            explicitNulls = true,
            coerceInputValues = true
        )

        /** Более «свободные» настройки, полезно для отладки/нестрогих API. */
        val LenientDebug = SerializationConfig(
            ignoreUnknownKeys = true,
            isLenient = true,
            encodeDefaults = true,
            allowStructuredMapKeys = true,
            prettyPrint = true,
            explicitNulls = true,
            coerceInputValues = true
        )
    }
}
