package ru.izhxx.aichallenge.core.dispatchers.impl

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import ru.izhxx.aichallenge.core.dispatchers.api.DispatchersProvider

internal class DispatchersProviderImpl : DispatchersProvider {
    override val main: CoroutineDispatcher = Dispatchers.Main
    override val io: CoroutineDispatcher = provideIODispatcher()
    override val default: CoroutineDispatcher = Dispatchers.Default
}
