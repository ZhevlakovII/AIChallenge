package ru.izhxx.aichallenge.core.network.core.impl.engine

import io.ktor.client.engine.HttpClientEngineFactory

/**
 * Платформенный выбор HttpClientEngineFactory.
 * В commonMain объявляем expect, actual будут в androidMain/jvmMain/iosMain.
 *
 * Для сборки common metadata actual не требуется.
 */
internal expect fun defaultEngineFactory(): HttpClientEngineFactory<*>
