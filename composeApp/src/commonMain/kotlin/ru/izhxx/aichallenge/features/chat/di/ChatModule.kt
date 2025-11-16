package ru.izhxx.aichallenge.features.chat.di

import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module
import ru.izhxx.aichallenge.features.chat.domain.usecase.CheckApiKeyConfigurationUseCase
import ru.izhxx.aichallenge.features.chat.domain.usecase.CheckApiKeyConfigurationUseCaseImpl
import ru.izhxx.aichallenge.features.chat.domain.usecase.SendMessageUseCase
import ru.izhxx.aichallenge.features.chat.domain.usecase.SendMessageUseCaseImpl
import ru.izhxx.aichallenge.features.chat.presentation.ChatViewModel
import ru.izhxx.aichallenge.features.chat.presentation.mapper.ChatResponseMapper

/**
 * Модуль DI для фичи чата
 */
val chatModule: Module = module {
    // Маппер
    factoryOf(::ChatResponseMapper)
    
    // UseCase
    factoryOf(::SendMessageUseCaseImpl) bind SendMessageUseCase::class
    factoryOf(::CheckApiKeyConfigurationUseCaseImpl) bind CheckApiKeyConfigurationUseCase::class

    // ViewModel
    viewModel { 
        ChatViewModel(
            sendMessageUseCase = get(),
            checkApiKeyConfigurationUseCase = get(),
            compressDialogHistoryUseCase = get(),
            llmConfigRepository = get(),
            metricsCacheRepository = get(),
            dialogPersistenceRepository = get(),
            responseMapper = get()
        )
    }
}
