package ru.izhxx.aichallenge.features.productassistant.impl.di

import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module
import ru.izhxx.aichallenge.features.productassistant.impl.data.datasource.LlmAnswerDataSource
import ru.izhxx.aichallenge.features.productassistant.impl.data.datasource.LlmAnswerDataSourceImpl
import ru.izhxx.aichallenge.features.productassistant.impl.data.datasource.RagSearchDataSource
import ru.izhxx.aichallenge.features.productassistant.impl.data.datasource.RagSearchDataSourceImpl
import ru.izhxx.aichallenge.features.productassistant.impl.data.datasource.TicketMcpDataSource
import ru.izhxx.aichallenge.features.productassistant.impl.data.datasource.TicketMcpDataSourceImpl
import ru.izhxx.aichallenge.features.productassistant.impl.data.repository.ProductAssistantRepositoryImpl
import ru.izhxx.aichallenge.features.productassistant.impl.domain.repository.ProductAssistantRepository
import ru.izhxx.aichallenge.features.productassistant.impl.domain.usecase.CreateTicketUseCase
import ru.izhxx.aichallenge.features.productassistant.impl.domain.usecase.CreateTicketUseCaseImpl
import ru.izhxx.aichallenge.features.productassistant.impl.domain.usecase.GenerateAnswerUseCase
import ru.izhxx.aichallenge.features.productassistant.impl.domain.usecase.GenerateAnswerUseCaseImpl
import ru.izhxx.aichallenge.features.productassistant.impl.domain.usecase.GetTicketUseCase
import ru.izhxx.aichallenge.features.productassistant.impl.domain.usecase.GetTicketUseCaseImpl
import ru.izhxx.aichallenge.features.productassistant.impl.domain.usecase.ListTicketsUseCase
import ru.izhxx.aichallenge.features.productassistant.impl.domain.usecase.ListTicketsUseCaseImpl
import ru.izhxx.aichallenge.features.productassistant.impl.domain.usecase.SearchFaqUseCase
import ru.izhxx.aichallenge.features.productassistant.impl.domain.usecase.SearchFaqUseCaseImpl
import ru.izhxx.aichallenge.features.productassistant.impl.domain.usecase.UpdateTicketUseCase
import ru.izhxx.aichallenge.features.productassistant.impl.domain.usecase.UpdateTicketUseCaseImpl
import ru.izhxx.aichallenge.features.productassistant.impl.presentation.ProductAssistantExecutor
import ru.izhxx.aichallenge.features.productassistant.impl.presentation.ProductAssistantReducer
import ru.izhxx.aichallenge.features.productassistant.impl.presentation.ProductAssistantViewModel
import ru.izhxx.aichallenge.features.productassistant.impl.presentation.mapper.ProductAssistantUiMapper

/**
 * Koin DI module for Product Assistant feature
 *
 * Note: RAG dependencies (RagSettingsRepository, RagIndexRepository, RagEmbedder, RagRetriever)
 * should be provided by the shared module and are expected to be available via get()
 */
val productAssistantModule = module {
    // Data Layer - Data Sources
    singleOf(::RagSearchDataSourceImpl) bind RagSearchDataSource::class
    singleOf(::TicketMcpDataSourceImpl) bind TicketMcpDataSource::class
    singleOf(::LlmAnswerDataSourceImpl) bind LlmAnswerDataSource::class

    // Data Layer - Repository
    singleOf(::ProductAssistantRepositoryImpl) bind ProductAssistantRepository::class

    // Domain Layer - Use Cases
    factoryOf(::SearchFaqUseCaseImpl) bind SearchFaqUseCase::class
    factoryOf(::GetTicketUseCaseImpl) bind GetTicketUseCase::class
    factoryOf(::ListTicketsUseCaseImpl) bind ListTicketsUseCase::class
    factoryOf(::GenerateAnswerUseCaseImpl) bind GenerateAnswerUseCase::class
    factoryOf(::CreateTicketUseCaseImpl) bind CreateTicketUseCase::class
    factoryOf(::UpdateTicketUseCaseImpl) bind UpdateTicketUseCase::class

    // Presentation Layer - Mapper
    singleOf(::ProductAssistantUiMapper)

    // Presentation Layer - Reducer
    singleOf(::ProductAssistantReducer)

    // Presentation Layer - Executor
    factoryOf(::ProductAssistantExecutor)

    // Presentation Layer - ViewModel
    viewModel { ProductAssistantViewModel(get()) }
}
