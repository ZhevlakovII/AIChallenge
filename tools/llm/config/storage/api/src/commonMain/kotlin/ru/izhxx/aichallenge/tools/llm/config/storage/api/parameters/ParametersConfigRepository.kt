package ru.izhxx.aichallenge.tools.llm.config.storage.api.parameters

import ru.izhxx.aichallenge.tools.llm.config.model.ParametersConfig

// TODO(заполнить документацию)
interface ParametersConfigRepository {

    suspend fun getConfig(): ParametersConfig

    suspend fun updateTemperature(temperature: Double)
    suspend fun updateMaxTokens(maxTokens: Int)
    suspend fun updateTopK(topK: Int)
    suspend fun updateTopP(topP: Double)
    suspend fun updateMinP(minP: Double)
    suspend fun updateTopA(topA: Double)
    suspend fun updateSeed(seed: Long)

    suspend fun updateConfig(newConfig: ParametersConfig)
}