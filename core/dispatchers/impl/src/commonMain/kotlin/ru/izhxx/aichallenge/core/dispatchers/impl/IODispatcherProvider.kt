package ru.izhxx.aichallenge.core.dispatchers.impl

import kotlinx.coroutines.CoroutineDispatcher

internal expect fun provideIODispatcher(): CoroutineDispatcher
