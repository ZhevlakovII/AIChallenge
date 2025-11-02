package ru.izhxx.aichallenge.di

import org.koin.dsl.module
import ru.izhxx.aichallenge.data.api.OpenAIClient

/**
 * Koin модуль для API-клиентов
 */
val apiModule = module {
    // OpenAIClient - клиент для работы с LLM API
    single { 
        OpenAIClient(llmSettingsStore = get())
    }
}
