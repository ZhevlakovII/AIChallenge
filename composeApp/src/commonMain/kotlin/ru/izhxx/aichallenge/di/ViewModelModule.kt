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
        ChatViewModel(apiKeyStore = get())
    }
    
    // SettingsViewModel - для экрана настроек
    viewModel {
        SettingsViewModel(apiKeyStore = get()) 
    }
}
