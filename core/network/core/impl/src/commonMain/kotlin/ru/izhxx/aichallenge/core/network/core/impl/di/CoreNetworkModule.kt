package ru.izhxx.aichallenge.core.network.core.impl.di

import org.koin.core.module.Module
import org.koin.dsl.module
import ru.izhxx.aichallenge.core.network.core.api.config.NetworkConfig
import ru.izhxx.aichallenge.core.network.core.api.factory.HttpClientFactory
import ru.izhxx.aichallenge.core.network.core.impl.factory.HttpClientFactoryImpl

/**
 * Koin module for Core Network layer.
 *
 * Provides:
 * - NetworkConfig (singleton, can be overridden)
 * - HttpClientFactory (singleton)
 */
val coreNetworkModule: Module = module {
    // Global network configuration (can be overridden by providing NetworkConfig before this module)
    single { NetworkConfig.Default }

    // HttpClient factory
    single<HttpClientFactory> { HttpClientFactoryImpl() }
}
