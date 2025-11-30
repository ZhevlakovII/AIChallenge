package ru.izhxx.aichallenge.core.network.core.impl.engine

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.cio.CIO

internal actual fun defaultEngineFactory(): HttpClientEngineFactory<*> = CIO
