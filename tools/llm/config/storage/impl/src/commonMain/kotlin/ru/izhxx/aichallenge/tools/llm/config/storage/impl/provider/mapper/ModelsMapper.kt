package ru.izhxx.aichallenge.tools.llm.config.storage.impl.provider.mapper

import ru.izhxx.aichallenge.core.url.Url
import ru.izhxx.aichallenge.tools.llm.config.model.ProviderConfig
import ru.izhxx.aichallenge.tools.llm.config.storage.impl.provider.entity.ProviderConfigEntity

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