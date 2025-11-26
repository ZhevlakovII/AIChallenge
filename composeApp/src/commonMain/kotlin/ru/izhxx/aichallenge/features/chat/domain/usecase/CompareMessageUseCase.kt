package ru.izhxx.aichallenge.features.chat.domain.usecase

import ru.izhxx.aichallenge.domain.model.MessageRole
import ru.izhxx.aichallenge.domain.model.message.LLMMessage
import ru.izhxx.aichallenge.domain.model.response.LLMResponse
import ru.izhxx.aichallenge.domain.repository.LLMClientRepository
import ru.izhxx.aichallenge.domain.repository.ProviderSettingsRepository
import ru.izhxx.aichallenge.domain.rag.RagEmbedder
import ru.izhxx.aichallenge.domain.rag.RagIndexRepository
import ru.izhxx.aichallenge.domain.rag.RagRetriever
import ru.izhxx.aichallenge.domain.rag.RagSettingsRepository
import ru.izhxx.aichallenge.domain.rag.RerankMode
import ru.izhxx.aichallenge.domain.rag.CutoffMode
import kotlin.time.TimeSource
import ru.izhxx.aichallenge.data.rag.RagSearchPipeline

/**
 * Результат сравнения ответов: без RAG и с RAG, плюс метрики ретрива.
 */
data class CompareResult(
    val baseline: LLMResponse,
    val rag: LLMResponse,
    val baselineRetrievalTimeMs: Long,
    val baselineUsedChunks: List<String>,
    val filteredRetrievalTimeMs: Long,
    val filteredUsedChunks: List<String>
)

interface CompareMessageUseCase {
    suspend operator fun invoke(
        text: String,
        previousMessages: List<LLMMessage>,
        summary: String?
    ): Result<CompareResult>
}

class CompareMessageUseCaseImpl(
    private val llmClientRepository: LLMClientRepository,
    private val providerSettingsRepository: ProviderSettingsRepository,
    private val ragSettingsRepository: RagSettingsRepository,
    private val ragIndexRepository: RagIndexRepository,
    private val ragEmbedder: RagEmbedder,
    private val ragRetriever: RagRetriever
) : CompareMessageUseCase {

    override suspend fun invoke(
        text: String,
        previousMessages: List<LLMMessage>,
        summary: String?
    ): Result<CompareResult> {
        // Проверяем наличие API ключа
        val apiKey = providerSettingsRepository.getApiKey()
        if (apiKey.isEmpty()) {
            return Result.failure(IllegalStateException("API ключ не настроен"))
        }

        // Сообщения для запроса (как в SendMessageUseCaseImpl)
        val lastMessage = previousMessages.lastOrNull()
        val messagesForRequest = if (lastMessage?.role == MessageRole.USER && lastMessage.content == text) {
            previousMessages
        } else {
            previousMessages + LLMMessage(role = MessageRole.USER, content = text)
        }

        // 1) RAG: получить настройки и индекс
        val settings = ragSettingsRepository.getSettings()
        val index = ragIndexRepository.getCurrentIndex() ?: run {
            val idxPath = settings.indexPath
            if (idxPath.isNullOrBlank()) {
                return Result.failure(IllegalStateException("Для сравнения нужен индекс RAG. Укажите путь в настройках."))
            }
            ragIndexRepository.loadIndex(idxPath).getOrElse { e ->
                return Result.failure(IllegalStateException("Не удалось загрузить RAG-индекс: ${e.message}", e))
            }
        }
        // 2) Подготовка пайплайна и "базовых" настроек без второго этапа (без rerank и без cutoff)
        val pipeline = RagSearchPipeline(
            embedder = ragEmbedder,
            retriever = ragRetriever
        )
        val baseSettings = settings.copy(
            rerank = settings.rerank.copy(
                mode = RerankMode.None,
                cutoffMode = CutoffMode.Quantile,
                quantileQ = 0.0
            )
        )
        val baseMark = TimeSource.Monotonic.markNow()
        val baseChunks = try {
            pipeline.retrieveChunks(
                questionText = text,
                index = index,
                settings = baseSettings
            )
        } catch (e: Throwable) {
            return Result.failure(IllegalStateException("RAG (без фильтра): pipeline ошибка: ${e.message}", e))
        }
        val baseRetrievalTime = baseMark.elapsedNow().inWholeMilliseconds
        val baseContext = pipeline.buildContext(
            chunks = baseChunks,
            index = index,
            settings = baseSettings
        )
        val baselineUsed = baseChunks.map { "${it.path}#${it.chunkIndex}" }
        val baselineMessages = buildList {
            if (messagesForRequest.isNotEmpty()) {
                addAll(messagesForRequest.dropLast(1))
                add(LLMMessage(role = MessageRole.SYSTEM, content = baseContext))
                add(messagesForRequest.last())
            } else {
                add(LLMMessage(role = MessageRole.SYSTEM, content = baseContext))
                add(LLMMessage(role = MessageRole.USER, content = text))
            }
        }
        val baselineRes = llmClientRepository.sendMessagesWithSummary(baselineMessages, summary)
            .getOrElse { return Result.failure(it) }


        val mark = TimeSource.Monotonic.markNow()
        val chunks = try {
            pipeline.retrieveChunks(
                questionText = text,
                index = index,
                settings = settings
            )
        } catch (e: Throwable) {
            return Result.failure(IllegalStateException("RAG: pipeline ошибка: ${e.message}", e))
        }
        val retrievalTime = mark.elapsedNow().inWholeMilliseconds

        val used = chunks.map { "${it.path}#${it.chunkIndex}" }

        val contextBlock = pipeline.buildContext(
            chunks = chunks,
            index = index,
            settings = settings
        )

        val augmentedMessages = buildList {
            if (messagesForRequest.isNotEmpty()) {
                addAll(messagesForRequest.dropLast(1))
                add(LLMMessage(role = MessageRole.SYSTEM, content = contextBlock))
                add(messagesForRequest.last())
            } else {
                add(LLMMessage(role = MessageRole.SYSTEM, content = contextBlock))
                add(LLMMessage(role = MessageRole.USER, content = text))
            }
        }

        val ragRes = llmClientRepository.sendMessagesWithSummary(augmentedMessages, summary)
            .getOrElse { return Result.failure(it) }
        return Result.success(
            CompareResult(
                baseline = baselineRes,
                rag = ragRes,
                baselineRetrievalTimeMs = baseRetrievalTime,
                baselineUsedChunks = baselineUsed,
                filteredRetrievalTimeMs = retrievalTime,
                filteredUsedChunks = used
            )
        )
    }
}
