package ru.izhxx.aichallenge.tools.llm.completions.impl.di

import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import org.koin.dsl.module
import ru.izhxx.aichallenge.core.network.api.HttpClientCreator
import ru.izhxx.aichallenge.tools.llm.completions.api.repository.CompletionsApiRepository
import ru.izhxx.aichallenge.tools.llm.completions.impl.repository.CompletionsApiRepositoryImpl

val interactionModule = module {
    single<CompletionsApiRepository> {
        CompletionsApiRepositoryImpl(
            httpClient = get<HttpClientCreator>().buildHttpClient {
                install(ContentNegotiation) {
                    json(json = get())
                }
            },
            json = get()
        )
    }
}
