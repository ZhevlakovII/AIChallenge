package ru.izhxx.aichallenge.tools.llm.config.storage.impl.provider

import ru.izhxx.aichallenge.core.url.Url
import ru.izhxx.aichallenge.tools.llm.config.model.ProviderConfig
import ru.izhxx.aichallenge.tools.llm.config.storage.api.provider.ProviderConfigRepository
import ru.izhxx.aichallenge.tools.llm.config.storage.impl.provider.dao.ProviderConfigDao
import ru.izhxx.aichallenge.tools.llm.config.storage.impl.provider.mapper.toDomain
import ru.izhxx.aichallenge.tools.llm.config.storage.impl.provider.mapper.toEntity

internal class ProviderConfigRepositoryImpl(
    private val providerConfigDao: ProviderConfigDao
) : ProviderConfigRepository {


    override suspend fun getConfig(): ProviderConfig = providerConfigDao.getConfig().toDomain()

    override suspend fun updateApiUrl(url: Url) {
        providerConfigDao.updateApiUrl(url.data)
    }

    override suspend fun updateModel(model: String) {
        providerConfigDao.updateModel(model)
    }

    override suspend fun updateApiKey(key: String) {
        providerConfigDao.updateApiKey(key)
    }

    override suspend fun updateConfig(config: ProviderConfig) {
        providerConfigDao.updateConfig(config.toEntity())
    }
}