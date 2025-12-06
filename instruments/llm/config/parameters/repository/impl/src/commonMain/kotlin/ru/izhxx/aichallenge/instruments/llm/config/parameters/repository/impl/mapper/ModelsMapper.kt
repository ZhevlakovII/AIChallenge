package ru.izhxx.aichallenge.instruments.llm.config.parameters.repository.impl.mapper

import ru.izhxx.aichallenge.instruments.llm.config.parameters.model.ParametersConfig
import ru.izhxx.aichallenge.instruments.llm.config.parameters.repository.impl.entity.ParametersConfigEntity

internal fun ParametersConfigEntity.toDomain(): ParametersConfig {
    return ParametersConfig(
        temperature = temperature,
        maxTokens = maxTokens,
        topK = topK,
        topP = topP,
        minP = minP,
        topA = topA,
        seed = seed
    )
}

internal fun ParametersConfig.toEntity(): ParametersConfigEntity {
    return ParametersConfigEntity(
        temperature = temperature,
        maxTokens = maxTokens,
        topK = topK,
        topP = topP,
        minP = minP,
        topA = topA,
        seed = seed
    )
}