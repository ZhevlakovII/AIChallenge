package ru.izhxx.aichallenge.core.network.api.config

import kotlinx.serialization.json.Json

/**
 * Конфигурация JSON-сериализации.
 */
data class JsonConfig(
    val ignoreUnknownKeys: Boolean = true,
    val isLenient: Boolean = true,
    val explicitNulls: Boolean = false,
    val encodeDefaults: Boolean = false,
) {
    fun toJson(): Json = Json {
        ignoreUnknownKeys = this@JsonConfig.ignoreUnknownKeys
        isLenient = this@JsonConfig.isLenient
        explicitNulls = this@JsonConfig.explicitNulls
        encodeDefaults = this@JsonConfig.encodeDefaults
    }
}
