package ru.izhxx.aichallenge.tools.llm.config.storage.impl.parameters

import ru.izhxx.aichallenge.tools.llm.config.model.ParametersConfig
import ru.izhxx.aichallenge.tools.llm.config.storage.api.parameters.ParametersConfigRepository
import ru.izhxx.aichallenge.tools.llm.config.storage.impl.parameters.dao.ParametersConfigDao
import ru.izhxx.aichallenge.tools.llm.config.storage.impl.parameters.mapper.toDomain
import ru.izhxx.aichallenge.tools.llm.config.storage.impl.parameters.mapper.toEntity

internal class ParametersConfigRepositoryImpl(
    private val parametersConfigDao: ParametersConfigDao
) : ParametersConfigRepository {

    override suspend fun getConfig(): ParametersConfig = parametersConfigDao.getConfigEntity().toDomain()

    override suspend fun updateTemperature(temperature: Double) {
        parametersConfigDao.updateTemperature(temperature)
    }

    override suspend fun updateMaxTokens(maxTokens: Int) {
        parametersConfigDao.updateMaxTokens(maxTokens)
    }

    override suspend fun updateTopK(topK: Int) {
        parametersConfigDao.updateTopK(topK)
    }

    override suspend fun updateTopP(topP: Double) {
        parametersConfigDao.updateTopP(topP)
    }

    override suspend fun updateMinP(minP: Double) {
        parametersConfigDao.updateMinP(minP)
    }

    override suspend fun updateTopA(topA: Double) {
        parametersConfigDao.updateTopA(topA)
    }

    override suspend fun updateSeed(seed: Long) {
        parametersConfigDao.updateSeed(seed)
    }

    override suspend fun updateConfig(newConfig: ParametersConfig) {
        parametersConfigDao.updateConfig(newConfig.toEntity())
    }
}