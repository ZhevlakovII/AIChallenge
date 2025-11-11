package ru.izhxx.aichallenge.di

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import ru.izhxx.aichallenge.viewmodel.ChatViewModel
import ru.izhxx.aichallenge.viewmodel.SettingsViewModel

/**
 * Koin модуль для ViewModels
 */
val viewModelModule = module {
    // ChatViewModel - для экрана чата
    viewModel { 
        ChatViewModel(
            llmClientRepository = get(),
            llmProviderSettingsRepositoryImpl = get(),
            llmPromptSettingsRepositoryImpl = get(),
        )
    }
    
    // SettingsViewModel - для экрана настроек
    viewModel {
        SettingsViewModel(
            providerSettingsStore = get(),
            promptSettingsStore = get(),
        ) 
    }
}
