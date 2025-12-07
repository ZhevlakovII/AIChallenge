package ru.izhxx.aichallenge.instruments.llm.config.parameters.repository.impl

import ru.izhxx.aichallenge.instruments.llm.config.parameters.model.ParametersConfig
import ru.izhxx.aichallenge.instruments.llm.config.parameters.repository.api.ParametersConfigRepository
import ru.izhxx.aichallenge.instruments.llm.config.parameters.repository.impl.dao.ParametersConfigDao
import ru.izhxx.aichallenge.instruments.llm.config.parameters.repository.impl.mapper.toDomain
import ru.izhxx.aichallenge.instruments.llm.config.parameters.repository.impl.mapper.toEntity

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