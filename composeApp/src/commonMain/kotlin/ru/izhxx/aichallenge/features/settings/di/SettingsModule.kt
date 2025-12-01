package ru.izhxx.aichallenge.features.settings.di

import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import ru.izhxx.aichallenge.features.settings.SettingsViewModel

// Ожидаем platform-specific модуль для RAG use cases
internal expect val ragPlatformModule: Module

val settingsModule = module {
    includes(ragPlatformModule)

    // SettingsViewModel - для экрана настроек
    viewModel {
        SettingsViewModel(
            providerSettingsStore = get(),
            lLMConfigRepository = get(),
            ragSettingsRepository = get(),
            indexRagDocumentsUseCase = getOrNull() // Optional: only on JVM
        )
    }
}
