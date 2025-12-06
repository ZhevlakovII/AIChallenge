package ru.izhxx.aichallenge.instruments.llm.config.provider.repository.impl.mapper

import ru.izhxx.aichallenge.core.url.Url
import ru.izhxx.aichallenge.instruments.llm.config.provider.model.ProviderConfig
import ru.izhxx.aichallenge.instruments.llm.config.provider.repository.impl.entity.ProviderConfigEntity

internal fun ProviderConfigEntity.toDomain(): ProviderConfig {
    return ProviderConfig(
        apiUrl = Url(apiUrl),
        apiKey = apiKey,
        model = model
    )
}

internal fun ProviderConfig.toEntity(): ProviderConfigEntity {
    return ProviderConfigEntity(
        apiUrl = apiUrl.data,
        apiKey = apiKey,
        model = model
    )
}