package ru.izhxx.aichallenge.di

import kotlinx.serialization.json.Json
import org.koin.dsl.module
import ru.izhxx.aichallenge.data.api.OpenAIApi
import ru.izhxx.aichallenge.data.api.OpenAIApiImpl
import ru.izhxx.aichallenge.data.repository.LLMClientRepositoryImpl
import ru.izhxx.aichallenge.data.repository.LLMPromptSettingsRepositoryImpl
import ru.izhxx.aichallenge.data.repository.LLMProviderSettingsRepositoryImpl
import ru.izhxx.aichallenge.domain.repository.LLMClientRepository
import ru.izhxx.aichallenge.domain.repository.LLMPromptSettingsRepository
import ru.izhxx.aichallenge.domain.repository.LLMProviderSettingsRepository

val sharedModule = module {
    single<Json> {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }
    // OpenAIApi - интерфейс для работы с OpenAI API
    single<OpenAIApi> {
        OpenAIApiImpl(get())
    }

    // LLMClientRepository - репозиторий для работы с LLM клиентом
    single<LLMClientRepository> {
        LLMClientRepositoryImpl(
            openAIApi = get(),
            providerSettingsRepository = get(),
            promptSettingsRepository = get()
        )
    }

    single<LLMPromptSettingsRepository> {
        LLMPromptSettingsRepositoryImpl(
            dataStore = DataStoreProvider.providePreferencesDataStore("llm_promt_settings.preferences_pb")
        )
    }

    single<LLMProviderSettingsRepository> {
        LLMProviderSettingsRepositoryImpl(
            dataStore = DataStoreProvider.providePreferencesDataStore("llm_provider_settings.preferences_pb")
        )
    }
}