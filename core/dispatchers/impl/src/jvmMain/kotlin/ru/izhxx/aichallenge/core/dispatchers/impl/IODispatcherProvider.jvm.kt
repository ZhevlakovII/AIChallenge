package ru.izhxx.aichallenge.core.dispatchers.impl

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal actual fun provideIODispatcher(): CoroutineDispatcher = Dispatchers.IO
