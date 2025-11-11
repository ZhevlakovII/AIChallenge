package ru.izhxx.aichallenge.features.chat.di

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import ru.izhxx.aichallenge.features.chat.ChatViewModel

val chatModule = module {
    // ChatViewModel - для экрана чата
    viewModel {
        ChatViewModel(
            llmClientRepository = get(),
            llmProviderSettingsRepositoryImpl = get(),
            llmPromptSettingsRepositoryImpl = get(),
        )
    }
}