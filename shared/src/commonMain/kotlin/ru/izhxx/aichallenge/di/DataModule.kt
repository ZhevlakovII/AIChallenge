package ru.izhxx.aichallenge.di

import org.koin.dsl.module
import ru.izhxx.aichallenge.data.preferences.ApiKeyStore
import ru.izhxx.aichallenge.data.preferences.ApiKeyStoreFactory
import ru.izhxx.aichallenge.data.preferences.LLMSettingsStore
import ru.izhxx.aichallenge.data.preferences.LLMSettingsStoreFactory

/**
 * Koin модуль для хранилища данных (preferences)
 */
val dataModule = module {
    // ApiKeyStoreFactory - фабрика для создания ApiKeyStore
    single { 
        ApiKeyStoreFactory()
    }
    
    // ApiKeyStore - для хранения API ключа OpenAI
    single<ApiKeyStore> { 
        get<ApiKeyStoreFactory>().create()
    }
    
    // LLMSettingsStoreFactory - фабрика для создания LLMSettingsStore
    single { 
        LLMSettingsStoreFactory()
    }
    
    // LLMSettingsStore - для хранения настроек LLM
    single<LLMSettingsStore> { 
        get<LLMSettingsStoreFactory>().create()
    }
}
