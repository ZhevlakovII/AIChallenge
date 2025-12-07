package ru.izhxx.aichallenge.core.network.impl.di

import kotlinx.serialization.json.Json
import org.koin.dsl.module
import ru.izhxx.aichallenge.core.network.api.HttpClientCreator
import ru.izhxx.aichallenge.core.network.impl.HttpClientCreatorImpl

val networkModule = module {
    single<HttpClientCreator> { HttpClientCreatorImpl() }
    single<Json> {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }
}
