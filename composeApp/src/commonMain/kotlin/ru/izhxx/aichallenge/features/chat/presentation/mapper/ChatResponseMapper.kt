package ru.izhxx.aichallenge.features.chat.presentation.mapper

import ru.izhxx.aichallenge.domain.model.config.ResponseFormat
import ru.izhxx.aichallenge.domain.model.message.ParsedMessage
import ru.izhxx.aichallenge.domain.model.response.LLMChoice
import ru.izhxx.aichallenge.domain.model.response.LLMUsage
import ru.izhxx.aichallenge.features.chat.presentation.model.ChatUiMessage
import ru.izhxx.aichallenge.features.chat.presentation.model.MessageContent
import ru.izhxx.aichallenge.features.chat.presentation.model.MessageMetadata
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Маппер для преобразования доменных моделей в UI-модели
 */
@OptIn(ExperimentalUuidApi::class)
class ChatResponseMapper {

    /**
     * Преобразует ответ LLM в UI-сообщение ассистента
     * @param requestId идентификатор запроса (если есть)
     * @return UI-сообщение от ассистента
     */
    fun mapLLMResponseToUiMessage(
        choice: LLMChoice,
        responseFormat: ResponseFormat,
        usage: LLMUsage?
    ): ChatUiMessage.AssistantMessage {

        // Преобразуем разобранное сообщение в UI-контент
        val content = mapParsedMessageToContent(choice.parsedMessage)

        // Создаем метаданные на основе информации об использовании
        val metadata = usage?.let { usage ->
            MessageMetadata(
                responseTimeMs = usage.responseTimeMs,
                tokensInput = usage.promptTokens,
                tokensOutput = usage.completionTokens,
                tokensTotal = usage.totalTokens,
                responseFormat = responseFormat.name.lowercase()
            )
        }

        // Создаем и возвращаем UI-сообщение ассистента
        return ChatUiMessage.AssistantMessage(
            id = Uuid.random().toString(),
            content = content,
            metadata = metadata
        )
    }

    /**
     * Создает UI-сообщение пользователя
     * @param text текст сообщения пользователя
     * @param id идентификатор сообщения (если есть)
     * @return UI-сообщение пользователя
     */
    fun createUserUiMessage(
        text: String,
        isHasError: Boolean,
        id: String = Uuid.random().toString()
    ): ChatUiMessage.UserMessage {
        return ChatUiMessage.UserMessage(
            id = id,
            isHasError = isHasError,
            content = MessageContent.Plain(text)
        )
    }

    /**
     * Создает техническое UI-сообщение
     * @param text текст технического сообщения
     * @param id идентификатор сообщения (если есть)
     * @return техническое UI-сообщение
     */
    fun createTechnicalUiMessage(
        text: String,
    ): ChatUiMessage.TechnicalMessage {
        return ChatUiMessage.TechnicalMessage(
            id = Uuid.random().toString(),
            content = MessageContent.Plain(text)
        )
    }

    /**
     * Преобразует разобранное сообщение в UI-контент
     * @param parsedMessage разобранное сообщение или null
     * @return UI-контент соответствующего типа
     */
    private fun mapParsedMessageToContent(parsedMessage: ParsedMessage?): MessageContent {
        return when (parsedMessage) {
            is ParsedMessage.Structured -> MessageContent.Structured(
                summary = parsedMessage.summary,
                explanation = parsedMessage.explanation,
                code = parsedMessage.code,
                references = parsedMessage.references
            )

            is ParsedMessage.Markdown -> MessageContent.Markdown(
                nodes = parsedMessage.nodes
            )

            is ParsedMessage.Plain -> MessageContent.Plain(
                text = parsedMessage.text
            )

            null -> MessageContent.Plain(
                text = "Не удалось обработать ответ"
            )
        }
    }
}
