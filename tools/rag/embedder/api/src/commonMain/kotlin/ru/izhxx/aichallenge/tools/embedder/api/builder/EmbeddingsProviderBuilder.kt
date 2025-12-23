package ru.izhxx.aichallenge.tools.embedder.api.builder

import ru.izhxx.aichallenge.tools.embedder.api.EmbedderConfig
import ru.izhxx.aichallenge.tools.embedder.api.EmbeddingsProvider

interface EmbeddingsProviderBuilder {
    operator fun invoke(config: EmbedderConfig): EmbeddingsProvider
}
