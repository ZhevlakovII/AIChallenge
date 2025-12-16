package ru.izhxx.aichallenge.core.dispatchers.impl.di

import org.koin.dsl.module
import ru.izhxx.aichallenge.core.dispatchers.api.DispatchersProvider
import ru.izhxx.aichallenge.core.dispatchers.impl.DispatchersProviderImpl

val dispatchersModule = module {
    single<DispatchersProvider> { DispatchersProviderImpl() }
}
