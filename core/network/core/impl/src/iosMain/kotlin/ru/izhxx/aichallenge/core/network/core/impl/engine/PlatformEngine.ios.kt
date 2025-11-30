package ru.izhxx.aichallenge.core.network.core.impl.engine

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.darwin.Darwin

internal actual fun defaultEngineFactory(): HttpClientEngineFactory<*> = Darwin