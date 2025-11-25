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

/**
 * Результат сравнения ответов: без RAG и с RAG, плюс метрики ретрива.
 */
data class CompareResult(
    val baseline: LLMResponse,
    val rag: LLMResponse,
    val retrievalTimeMs: Long,
    val usedChunks: List<String> // path#chunkIndex
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

        // 1) BASELINE
        val baselineRes = llmClientRepository.sendMessagesWithSummary(messagesForRequest, summary)
            .getOrElse { return Result.failure(it) }

        // 2) RAG: подготовка
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

        val startRetrieval = kotlin.system.getTimeMillis()
        val qEmbedding = try {
            ragEmbedder.embed(text)
        } catch (e: Throwable) {
            return Result.failure(IllegalStateException("RAG: embedder недоступен: ${e.message}", e))
        }
        val retrieved = ragRetriever.retrieve(
            questionEmbedding = qEmbedding,
            index = index,
            topK = settings.topK,
            minScore = settings.minScore
        )
        val retrievalTime = kotlin.system.getTimeMillis() - startRetrieval

        val used = retrieved.map { "${it.path}#${it.chunkIndex}" }

        val contextBlock = ragRetriever.buildContext(
            chunks = retrieved,
            index = index,
            maxTokens = settings.maxContextTokens
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
                retrievalTimeMs = retrievalTime,
                usedChunks = used
            )
        )
    }
}
