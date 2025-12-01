package ru.izhxx.aichallenge.features.settings.di

import org.koin.core.module.Module
import org.koin.dsl.module
import ru.izhxx.aichallenge.features.rag.domain.IndexRagDocumentsUseCase
import ru.izhxx.aichallenge.features.rag.domain.IndexRagDocumentsUseCaseImpl

internal actual val ragPlatformModule: Module = module {
    // RAG Indexing Use Case (JVM-only)
    factory<IndexRagDocumentsUseCase> {
        IndexRagDocumentsUseCaseImpl(httpClient = get())
    }
}
