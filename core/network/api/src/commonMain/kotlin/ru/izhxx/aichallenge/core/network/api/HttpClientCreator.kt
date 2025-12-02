package ru.izhxx.aichallenge.core.network.api

import io.ktor.client.HttpClient

interface HttpClientCreator {

    fun buildHttpClient(): HttpClient
}