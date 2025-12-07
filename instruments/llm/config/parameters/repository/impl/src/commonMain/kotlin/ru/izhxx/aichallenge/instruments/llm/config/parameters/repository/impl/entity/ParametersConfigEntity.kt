package ru.izhxx.aichallenge.instruments.llm.config.parameters.repository.impl.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "llm_parameters_config"
)
internal class ParametersConfigEntity(
    @PrimaryKey(autoGenerate = true)
    internal val primaryKey: Long = 0L,
    val temperature: Double,
    val maxTokens: Int,
    val topK: Int,
    val topP: Double,
    val minP: Double,
    val topA: Double,
    val seed: Long,
)
