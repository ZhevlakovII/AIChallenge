package ru.izhxx.aichallenge.instruments.llm.config.provider.repository.impl

import ru.izhxx.aichallenge.core.url.Url
import ru.izhxx.aichallenge.instruments.llm.config.provider.model.ProviderConfig
import ru.izhxx.aichallenge.instruments.llm.config.provider.repository.api.ProviderConfigRepository
import ru.izhxx.aichallenge.instruments.llm.config.provider.repository.impl.dao.ProviderConfigDao
import ru.izhxx.aichallenge.instruments.llm.config.provider.repository.impl.mapper.toDomain
import ru.izhxx.aichallenge.instruments.llm.config.provider.repository.impl.mapper.toEntity

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