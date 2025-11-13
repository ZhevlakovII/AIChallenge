package ru.izhxx.aichallenge.test

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import ru.izhxx.aichallenge.domain.model.MessageRole
import ru.izhxx.aichallenge.domain.model.message.LLMMessage
import ru.izhxx.aichallenge.data.service.DialogHistoryCompressionServiceImpl
import ru.izhxx.aichallenge.data.usecase.CompressDialogHistoryUseCaseImpl
import ru.izhxx.aichallenge.domain.model.response.LLMChoice
import ru.izhxx.aichallenge.domain.model.response.LLMResponse
import ru.izhxx.aichallenge.domain.model.response.LLMUsage
import ru.izhxx.aichallenge.domain.repository.DialogSummaryRepository
import ru.izhxx.aichallenge.domain.repository.LLMClientRepository
import ru.izhxx.aichallenge.domain.repository.LLMConfigRepository

/**
 * Тесты для механизма сжатия истории диалога
 */
class DialogHistoryCompressionTest {
    
    /**
     * Моковый репозиторий для суммаризации диалогов
     */
    class MockDialogSummaryRepository : DialogSummaryRepository {
        var lastMessages: List<LLMMessage> = emptyList()
        var tokenCount: Int = 0
        
        override suspend fun createSummary(messages: List<LLMMessage>): Result<String> {
            lastMessages = messages
            
            // Подсчитываем токены (примерно 1 токен на 4 символа)
            var totalChars = 0
            messages.forEach { message ->
                totalChars += message.content.length
            }
            tokenCount = totalChars / 4
            
            // Возвращаем моковую суммаризацию
            return Result.success("Это суммаризация разговора: пользователь обсуждал Android разработку.")
        }
    }
    
    /**
     * Моковый репозиторий для LLM клиента, который используется для тестирования токенов
     */
    class MockLLMClientRepository : LLMClientRepository {
        var lastSummary: String? = null
        var lastMessages: List<LLMMessage> = emptyList()
        var tokenCount: Int = 0
        
        override suspend fun sendMessages(messages: List<LLMMessage>): Result<LLMResponse> {
            return sendMessagesWithSummary(messages, null)
        }
        
        override suspend fun sendMessagesWithSummary(
            messages: List<LLMMessage>, 
            summary: String?
        ): Result<LLMResponse> {
            lastSummary = summary
            lastMessages = messages
            
            // Подсчитываем токены (примерно 1 токен на 4 символа)
            var totalChars = 0
            messages.forEach { message ->
                totalChars += message.content.length
            }
            if (summary != null) {
                totalChars += summary.length
            }
            tokenCount = totalChars / 4
            
            // Возвращаем моковый ответ
            val responseContent = "Ответ на запрос"
            
            return Result.success(
                LLMResponse(
                    id = "test-id",
                    choices = listOf(
                        LLMChoice(
                            index = 0,
                            rawMessage = LLMMessage(
                                role = MessageRole.ASSISTANT,
                                content = responseContent
                            ),
                            parsedMessage = responseContent,
                            finishReason = "stop"
                        )
                    ),
                    format = "text",
                    usage = LLMUsage(
                        promptTokens = tokenCount,
                        completionTokens = 50,
                        totalTokens = tokenCount + 50,
                        responseTimeMs = 100
                    )
                )
            )
        }
    }
    
    /**
     * Моковый репозиторий настроек LLM
     */
    class MockLLMConfigRepository : LLMConfigRepository {
        override suspend fun getSettings() = object {
            val systemPrompt = "Ты полезный помощник"
            val responseFormat = "text"
            val temperature = 1.0f
            val maxTokens = 2048
            val topK = 0
            val topP = 0.0f
            val minP = 0.0f
            val topA = 0.0f
            val seed = 0L
        }
        
        override suspend fun updateSettings(update: suspend (Any) -> Any): Result<Unit> {
            return Result.success(Unit)
        }
    }
    
    @Test
    fun `история меньше порога не сжимается`() = runTest {
        // Подготовка
        val mockDialogSummaryRepository = MockDialogSummaryRepository()
        val mockLLMConfigRepository = MockLLMConfigRepository()
        val service = DialogHistoryCompressionServiceImpl(mockDialogSummaryRepository, mockLLMConfigRepository)
        val useCase = CompressDialogHistoryUseCaseImpl(service)
        
        // Создаем историю диалога меньше порога (4 сообщения)
        val messages = listOf(
            LLMMessage(role = MessageRole.USER, content = "Привет, как дела?"),
            LLMMessage(role = MessageRole.ASSISTANT, content = "Отлично! Чем могу помочь?"),
            LLMMessage(role = MessageRole.USER, content = "Расскажи про Kotlin"),
            LLMMessage(role = MessageRole.ASSISTANT, content = "Kotlin - современный язык программирования.")
        )
        
        // Действие
        val (summary, compressedMessages) = useCase(messages)
        
        // Проверка
        assertNull(summary, "Не должно быть суммаризации для истории меньше порога")
        assertEquals(messages, compressedMessages, "История не должна измениться")
    }
    
    @Test
    fun `история больше порога сжимается`() = runTest {
        // Подготовка
        val mockDialogSummaryRepository = MockDialogSummaryRepository()
        val mockLLMConfigRepository = MockLLMConfigRepository()
        val service = DialogHistoryCompressionServiceImpl(mockDialogSummaryRepository, mockLLMConfigRepository)
        val useCase = CompressDialogHistoryUseCaseImpl(service)
        
        // Создаем историю диалога больше порога (6 сообщений)
        val messages = listOf(
            LLMMessage(role = MessageRole.USER, content = "Привет, как дела?"),
            LLMMessage(role = MessageRole.ASSISTANT, content = "Отлично! Чем могу помочь?"),
            LLMMessage(role = MessageRole.USER, content = "Расскажи про Kotlin"),
            LLMMessage(role = MessageRole.ASSISTANT, content = "Kotlin - современный язык программирования."),
            LLMMessage(role = MessageRole.USER, content = "А что такое Coroutines?"),
            LLMMessage(role = MessageRole.ASSISTANT, content = "Coroutines - это компонент для асинхронного программирования.")
        )
        
        // Действие
        val (summary, compressedMessages) = useCase(messages)
        
        // Проверка
        assertNotNull(summary, "Должна быть создана суммаризация для истории больше порога")
        assertTrue(compressedMessages.size < messages.size, "Сжатая история должна содержать меньше сообщений")
        
        // Проверяем, что последние сообщения сохранены
        assertEquals(
            messages.last().content,
            compressedMessages.last().content,
            "Последнее сообщение должно сохраниться в сжатой истории"
        )
    }
    
    @Test
    fun `сравнение использования токенов`() = runTest {
        // Подготовка
        val mockLLMRepository = MockLLMClientRepository()
        val mockDialogSummaryRepository = MockDialogSummaryRepository()
        val mockLLMConfigRepository = MockLLMConfigRepository()
        val service = DialogHistoryCompressionServiceImpl(mockDialogSummaryRepository, mockLLMConfigRepository)
        val useCase = CompressDialogHistoryUseCaseImpl(service)
        
        // Создаем длинную историю диалога (10 сообщений)
        val messages = buildList {
            repeat(10) { i ->
                add(LLMMessage(
                    role = if (i % 2 == 0) MessageRole.USER else MessageRole.ASSISTANT,
                    content = "Сообщение №$i. Это длинное сообщение с информацией о Android разработке, Kotlin, Jetpack Compose и многом другом. ".repeat(5)
                ))
            }
        }
        
        // Сначала отправляем все сообщения без сжатия
        val tokensBeforeCompression = calculateTokens(mockLLMRepository, messages, null)
        
        // Теперь сжимаем историю и смотрим, сколько токенов нужно после сжатия
        val (summary, compressedMessages) = useCase(messages)
        val tokensAfterCompression = calculateTokens(mockLLMRepository, compressedMessages, summary)
        
        // Проверка
        println("Токенов до сжатия: $tokensBeforeCompression")
        println("Токенов после сжатия: $tokensAfterCompression")
        assertTrue(tokensAfterCompression < tokensBeforeCompression, "Сжатие должно уменьшать количество токенов")
        
        // Проверка примерной эффективности сжатия (должно быть хотя бы 30% экономии)
        val compressionEfficiency = 1.0 - (tokensAfterCompression.toDouble() / tokensBeforeCompression.toDouble())
        println("Эффективность сжатия: ${(compressionEfficiency * 100).toInt()}%")
        assertTrue(compressionEfficiency > 0.3, "Эффективность сжатия должна быть не менее 30%")
    }
    
    /**
     * Вспомогательная функция для подсчета токенов
     */
    private suspend fun calculateTokens(
        repository: MockLLMClientRepository,
        messages: List<LLMMessage>,
        summary: String?
    ): Int {
        repository.sendMessagesWithSummary(messages, summary)
        return repository.tokenCount
    }
}
