package ru.izhxx.aichallenge.instruments.llm.config.parameters.repository.impl.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import ru.izhxx.aichallenge.instruments.llm.config.parameters.repository.impl.entity.ParametersConfigEntity

@Dao
internal interface ParametersConfigDao {

    @Query("SELECT * FROM llm_parameters_config")
    suspend fun getConfigEntity(): ParametersConfigEntity

    @Query("UPDATE llm_parameters_config SET temperature = :temperature")
    suspend fun updateTemperature(temperature: Double)

    @Query("UPDATE llm_parameters_config SET maxTokens = :tokens")
    suspend fun updateMaxTokens(tokens: Int)

    @Query("UPDATE llm_parameters_config SET topK = :topK")
    suspend fun updateTopK(topK: Int)

    @Query("UPDATE llm_parameters_config SET topP = :topP")
    suspend fun updateTopP(topP: Double)

    @Query("UPDATE llm_parameters_config SET minP = :minP")
    suspend fun updateMinP(minP: Double)

    @Query("UPDATE llm_parameters_config SET topA = :topA")
    suspend fun updateTopA(topA: Double)

    @Query("UPDATE llm_parameters_config SET seed = :seed")
    suspend fun updateSeed(seed: Long)

    @Upsert(entity = ParametersConfigEntity::class)
    suspend fun updateConfig(newConfig: ParametersConfigEntity)
}
