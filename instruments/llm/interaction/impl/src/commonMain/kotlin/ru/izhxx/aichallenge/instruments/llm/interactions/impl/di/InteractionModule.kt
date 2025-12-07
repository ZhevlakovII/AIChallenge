package ru.izhxx.aichallenge.instruments.llm.interactions.impl.di

import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import org.koin.dsl.module
import ru.izhxx.aichallenge.core.network.api.HttpClientCreator
import ru.izhxx.aichallenge.instruments.llm.interactions.api.repository.InteractionRepository
import ru.izhxx.aichallenge.instruments.llm.interactions.impl.repository.InteractionRepositoryImpl

val interactionModule = module {
    single<InteractionRepository> {
        InteractionRepositoryImpl(
            httpClient = get<HttpClientCreator>().buildHttpClient {
                install(ContentNegotiation) {
                    json(get())
                }
            },
            json = get()
        )
    }
}
