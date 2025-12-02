package ru.izhxx.aichallenge.features.settings.di

import org.koin.core.module.Module
import org.koin.dsl.module

// Android не поддерживает RAG индексацию (требует JVM filesystem)
internal actual val ragPlatformModule: Module = module {
    // Пустой модуль для Android
}
