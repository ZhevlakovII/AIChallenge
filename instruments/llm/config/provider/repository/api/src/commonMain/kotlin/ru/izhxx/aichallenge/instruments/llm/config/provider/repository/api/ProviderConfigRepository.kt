package ru.izhxx.aichallenge.instruments.llm.config.provider.repository.api

import ru.izhxx.aichallenge.core.url.Url
import ru.izhxx.aichallenge.instruments.llm.config.provider.model.ProviderConfig

// TODO(заполнить документацию)
interface ProviderConfigRepository {

    suspend fun getConfig(): ProviderConfig

    suspend fun updateApiUrl(url: Url)
    suspend fun updateModel(model: String)
    suspend fun updateApiKey(key: String)

    suspend fun updateConfig(config: ProviderConfig)
}