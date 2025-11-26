package ru.izhxx.aichallenge.features.settings.di

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import ru.izhxx.aichallenge.features.settings.SettingsViewModel

val settingsModule = module {
    // SettingsViewModel - для экрана настроек
    viewModel {
        SettingsViewModel(
            providerSettingsStore = get(),
            lLMConfigRepository = get(),
            ragSettingsRepository = get()
        )
    }
}
