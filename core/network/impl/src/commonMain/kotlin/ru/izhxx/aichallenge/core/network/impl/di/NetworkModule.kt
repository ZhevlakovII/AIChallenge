package ru.izhxx.aichallenge.core.network.impl.di

import org.koin.dsl.module
import ru.izhxx.aichallenge.core.network.api.HttpClientCreator
import ru.izhxx.aichallenge.core.network.impl.HttpClientCreatorImpl

val networkModule = module {
    single<HttpClientCreator> { HttpClientCreatorImpl() }
}
