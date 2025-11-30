package ru.izhxx.aichallenge.core.network.core.impl.engine

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.okhttp.OkHttp

internal actual fun defaultEngineFactory(): HttpClientEngineFactory<*> = OkHttp
