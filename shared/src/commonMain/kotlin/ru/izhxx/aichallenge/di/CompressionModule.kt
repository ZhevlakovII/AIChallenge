package ru.izhxx.aichallenge.di

import org.koin.dsl.module
import ru.izhxx.aichallenge.data.repository.DialogSummaryRepositoryImpl
import ru.izhxx.aichallenge.data.service.DialogHistoryCompressionServiceImpl
import ru.izhxx.aichallenge.data.usecase.CompressDialogHistoryUseCaseImpl
import ru.izhxx.aichallenge.domain.repository.DialogSummaryRepository
import ru.izhxx.aichallenge.domain.service.DialogHistoryCompressionService
import ru.izhxx.aichallenge.domain.usecase.CompressDialogHistoryUseCase

/**
 * Модуль для внедрения зависимостей, связанных с механизмом сжатия истории диалога
 */
val compressionModule = module {
    // Репозиторий для суммаризации диалогов
    single<DialogSummaryRepository> {
        DialogSummaryRepositoryImpl(
            openAIApi = get(),
            providerSettingsRepository = get()
        )
    }
    
    // Сервис для сжатия истории диалога
    single<DialogHistoryCompressionService> {
        DialogHistoryCompressionServiceImpl(
            dialogSummaryRepository = get()
        )
    }
    
    // UseCase для сжатия истории диалога
    single<CompressDialogHistoryUseCase> {
        CompressDialogHistoryUseCaseImpl(
            compressionService = get()
        )
    }
}
