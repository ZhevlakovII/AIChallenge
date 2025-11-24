package ru.izhxx.aichallenge.di

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import ru.izhxx.aichallenge.data.api.OpenAIApi
import ru.izhxx.aichallenge.data.api.OpenAIApiImpl
import ru.izhxx.aichallenge.data.database.AppDatabase
import ru.izhxx.aichallenge.data.database.DatabaseFactory
import ru.izhxx.aichallenge.data.repository.DialogPersistenceRepositoryImpl
import ru.izhxx.aichallenge.data.repository.LLMClientRepositoryImpl
import ru.izhxx.aichallenge.data.repository.LLMConfigRepositoryImpl
import ru.izhxx.aichallenge.data.repository.ProviderSettingsRepositoryImpl
import ru.izhxx.aichallenge.data.repository.ReminderRepositoryImpl
import ru.izhxx.aichallenge.data.usecase.ExecuteReminderTaskUseCaseImpl
import ru.izhxx.aichallenge.domain.repository.DialogPersistenceRepository
import ru.izhxx.aichallenge.domain.repository.LLMClientRepository
import ru.izhxx.aichallenge.domain.repository.LLMConfigRepository
import ru.izhxx.aichallenge.domain.repository.ProviderSettingsRepository
import ru.izhxx.aichallenge.domain.repository.ReminderRepository
import ru.izhxx.aichallenge.domain.service.NoopReminderNotifier
import ru.izhxx.aichallenge.domain.service.ReminderEngine
import ru.izhxx.aichallenge.domain.service.ReminderNotifier
import ru.izhxx.aichallenge.domain.usecase.ExecuteReminderTaskUseCase

val sharedModule = module {
    single<Json> {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }
    single<HttpClient> {
        HttpClient {
            install(ContentNegotiation) {
                json(get())
            }
            install(WebSockets)
            install(HttpTimeout) {
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 120_000
                requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
            }
        }
    }
    // OpenAIApi - интерфейс для работы с OpenAI API
    single<OpenAIApi> {
        OpenAIApiImpl(get())
    }

    // LLMClientRepository - репозиторий для работы с LLM клиентом
    single<LLMClientRepository> {
        LLMClientRepositoryImpl(
            openAIApi = get(),
            llmConfigRepository = get(),
            providerSettingsRepository = get(),
            resultParser = get(),
            mcpRepository = get(),
            mcpConfigRepository = get(),
            toolsMapper = get(),
            json = get(),
            mcpRouter = get(),
            getMcpServers = get()
        )
    }

    single<LLMConfigRepository> {
        LLMConfigRepositoryImpl(
            dataStore = DataStoreProvider.providePreferencesDataStore("llm_promt_settings.preferences_pb")
        )
    }

    single<ProviderSettingsRepository> {
        ProviderSettingsRepositoryImpl(
            dataStore = DataStoreProvider.providePreferencesDataStore("llm_provider_settings.preferences_pb")
        )
    }

    single<AppDatabase> {
        DatabaseFactory.getDatabase()
    }

    // DialogPersistenceRepository - репозиторий для работы с сохраненными диалогами
    single<DialogPersistenceRepository> {
        DialogPersistenceRepositoryImpl(get())
    }

    // Reminder: репозиторий, use case, notifier (по умолчанию no-op), движок
    single<ReminderRepository> { ReminderRepositoryImpl(get()) }
    single<ExecuteReminderTaskUseCase> { ExecuteReminderTaskUseCaseImpl(get()) }
    single<ReminderNotifier> { NoopReminderNotifier() }
    single { ReminderEngine(get(), get(), get(), maxParallelTasks = 3) }

    includes(
        parsersModule,
        compressionModule,
        metricsModule,
        mcpSharedModule
    )
}
