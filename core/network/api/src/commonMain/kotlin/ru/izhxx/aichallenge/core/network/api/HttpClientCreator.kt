package ru.izhxx.aichallenge.core.network.api

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig

// TODO(Необходима документация)
interface HttpClientCreator {

    fun buildHttpClient(block: HttpClientConfig<*>.() -> Unit): HttpClient
}
