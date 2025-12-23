package ru.izhxx.aichallenge.tools.llm.config.storage.impl.parameters.mapper

import ru.izhxx.aichallenge.tools.llm.config.model.ParametersConfig
import ru.izhxx.aichallenge.tools.llm.config.storage.impl.parameters.entity.ParametersConfigEntity

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