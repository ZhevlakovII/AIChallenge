package ru.izhxx.aichallenge.features.chat.domain.usecase

import ru.izhxx.aichallenge.domain.model.MessageRole
import ru.izhxx.aichallenge.domain.model.message.LLMMessage
import ru.izhxx.aichallenge.domain.model.response.LLMResponse
import ru.izhxx.aichallenge.domain.repository.LLMClientRepository
import ru.izhxx.aichallenge.domain.repository.ProviderSettingsRepository
import ru.izhxx.aichallenge.domain.rag.RagSettingsRepository
import ru.izhxx.aichallenge.domain.rag.RagIndexRepository
import ru.izhxx.aichallenge.domain.rag.RagEmbedder
import ru.izhxx.aichallenge.domain.rag.RagRetriever
import ru.izhxx.aichallenge.data.rag.RagSearchPipeline

/**
 * Реализация юзкейса отправки сообщения
 */
class SendMessageUseCaseImpl(
    private val llmClientRepository: LLMClientRepository,
    private val providerSettingsRepository: ProviderSettingsRepository,
    private val ragSettingsRepository: RagSettingsRepository,
    private val ragIndexRepository: RagIndexRepository,
    private val ragEmbedder: RagEmbedder,
    private val ragRetriever: RagRetriever
) : SendMessageUseCase {

    override suspend fun invoke(
        text: String,
        previousMessages: List<LLMMessage>
    ): Result<LLMResponse> {
        // Вызываем перегруженную версию метода без суммаризации
        return invoke(text, previousMessages, null)
    }
    
    override suspend fun invoke(
        text: String,
        previousMessages: List<LLMMessage>,
        summary: String?
    ): Result<LLMResponse> {
        // Проверяем наличие API ключа
        val apiKey = providerSettingsRepository.getApiKey()
        if (apiKey.isEmpty()) {
            return Result.failure(IllegalStateException("API ключ не настроен"))
        }

        // Проверяем, содержит ли последнее сообщение текст, который мы отправляем
        val lastMessage = previousMessages.lastOrNull()
        val messagesForRequest = if (lastMessage?.role == MessageRole.USER && lastMessage.content == text) {
            // Если последнее сообщение уже содержит этот текст, используем сообщения как есть
            previousMessages
        } else {
            // Иначе добавляем новое сообщение пользователя
            val userMessage = LLMMessage(
                role = MessageRole.USER,
                content = text
            )
            previousMessages + userMessage
        }

        // RAG pipeline
        val ragSettings = ragSettingsRepository.getSettings()
        if (!ragSettings.enabled) {
            // Без RAG — стандартный путь
            return llmClientRepository.sendMessagesWithSummary(messagesForRequest, summary)
        }

        // Гарантируем загруженный индекс
        val index = ragIndexRepository.getCurrentIndex() ?: run {
            val idxPath = ragSettings.indexPath
            if (idxPath.isNullOrBlank()) {
                return Result.failure(IllegalStateException("RAG включен, но не настроен путь к индексу"))
            }
            ragIndexRepository.loadIndex(idxPath).getOrElse { e ->
                return Result.failure(IllegalStateException("Не удалось загрузить RAG-индекс: ${e.message}", e))
            }
        }

        // Второй этап: пайплайн RAG (kNN -> rerank/cutoff -> topK)
        val pipeline = RagSearchPipeline(
            embedder = ragEmbedder,
            retriever = ragRetriever
        )
        val chunks = try {
            pipeline.retrieveChunks(
                questionText = text,
                index = index,
                settings = ragSettings
            )
        } catch (e: Throwable) {
            return Result.failure(IllegalStateException("RAG: pipeline ошибка: ${e.message}", e))
        }

        if (chunks.isEmpty()) {
            // Нет релевантных чанков — отправляем без контекста
            return llmClientRepository.sendMessagesWithSummary(messagesForRequest, summary)
        }

        // Сборка контекстного блока
        val contextBlock = pipeline.buildContext(
            chunks = chunks,
            index = index,
            settings = ragSettings
        )

        // Вставляем CONTEXT перед пользовательским вопросом
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

        return llmClientRepository.sendMessagesWithSummary(augmentedMessages, summary)
    }
}
