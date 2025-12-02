package ru.izhxx.aichallenge.features.pranalyzer.impl.di

import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module
import ru.izhxx.aichallenge.features.pranalyzer.impl.data.datasource.LlmAnalysisDataSource
import ru.izhxx.aichallenge.features.pranalyzer.impl.data.datasource.LlmAnalysisDataSourceImpl
import ru.izhxx.aichallenge.features.pranalyzer.impl.data.datasource.PrMcpDataSource
import ru.izhxx.aichallenge.features.pranalyzer.impl.data.datasource.PrMcpDataSourceImpl
import ru.izhxx.aichallenge.features.pranalyzer.impl.data.repository.PrAnalyzerRepositoryImpl
import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.repository.PrAnalyzerRepository
import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.usecase.AnalyzePrWithLlmUseCase
import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.usecase.AnalyzePrWithLlmUseCaseImpl
import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.usecase.FetchPrDiffUseCase
import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.usecase.FetchPrDiffUseCaseImpl
import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.usecase.FetchPrInfoUseCase
import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.usecase.FetchPrInfoUseCaseImpl
import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.usecase.GenerateReportUseCase
import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.usecase.GenerateReportUseCaseImpl
import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.usecase.SearchRelevantDocsUseCase
import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.usecase.SearchRelevantDocsUseCaseImpl
import ru.izhxx.aichallenge.features.pranalyzer.impl.presentation.PrAnalyzerExecutor
import ru.izhxx.aichallenge.features.pranalyzer.impl.presentation.PrAnalyzerViewModel
import ru.izhxx.aichallenge.features.pranalyzer.impl.presentation.mapper.PrAnalysisUiMapper
import kotlin.time.ExperimentalTime

/**
 * Koin DI module for PR Analyzer feature
 */
@OptIn(ExperimentalTime::class)
val prAnalyzerModule = module {
    // Data Layer - Data Sources
    singleOf(::PrMcpDataSourceImpl) bind PrMcpDataSource::class
    singleOf(::LlmAnalysisDataSourceImpl) bind LlmAnalysisDataSource::class

    // Data Layer - Repository
    singleOf(::PrAnalyzerRepositoryImpl) bind PrAnalyzerRepository::class

    // Domain Layer - Use Cases
    factoryOf(::FetchPrInfoUseCaseImpl) bind FetchPrInfoUseCase::class
    factoryOf(::FetchPrDiffUseCaseImpl) bind FetchPrDiffUseCase::class
    factoryOf(::SearchRelevantDocsUseCaseImpl) bind SearchRelevantDocsUseCase::class
    factoryOf(::AnalyzePrWithLlmUseCaseImpl) bind AnalyzePrWithLlmUseCase::class
    factoryOf(::GenerateReportUseCaseImpl) bind GenerateReportUseCase::class

    // Presentation Layer - Mapper
    singleOf(::PrAnalysisUiMapper)

    // Presentation Layer - Executor
    factoryOf(::PrAnalyzerExecutor)

    // Presentation Layer - ViewModel
    viewModel { PrAnalyzerViewModel(get()) }
}
