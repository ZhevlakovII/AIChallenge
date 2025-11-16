package ru.izhxx.aichallenge.data.repository

import ru.izhxx.aichallenge.common.Logger
import ru.izhxx.aichallenge.common.safeApiCall
import ru.izhxx.aichallenge.data.api.OpenAIApi
import ru.izhxx.aichallenge.data.model.ChatMessageDTO
import ru.izhxx.aichallenge.data.model.LLMChatRequestDTO
import ru.izhxx.aichallenge.domain.model.MessageRole
import ru.izhxx.aichallenge.domain.model.message.LLMMessage
import ru.izhxx.aichallenge.domain.model.summary.DialogSummaryMetrics
import ru.izhxx.aichallenge.domain.repository.DialogSummaryRepository
import ru.izhxx.aichallenge.domain.repository.ProviderSettingsRepository

/**
 * Реализация репозитория для работы с суммаризацией диалогов
 */
class DialogSummaryRepositoryImpl(
    private val openAIApi: OpenAIApi,
    private val providerSettingsRepository: ProviderSettingsRepository
) : DialogSummaryRepository {
    
    // Создаем логгер
    private val logger = Logger.forClass(this)
    
    // Системный промпт для суммаризации диалога
    private val summarySystemPrompt = """
        Ты - эксперт по созданию информативных и компактных суммаризаций технических диалогов.
        Твоя цель - сбалансировать информативность и компактность, чтобы последующая LLM имела достаточный контекст для продолжения диалога.
        
        ТРЕБОВАНИЯ К СОДЕРЖАНИЮ:
        1. ИНФОРМАТИВНОСТЬ И СТРУКТУРА:
           - Сохраняй ключевые смысловые блоки диалога в их последовательности
           - Включай основные вопросы и проблемы, которые обсуждались
           - Отмечай достигнутые выводы и согласованные решения
           - Сохраняй логические связи между темами
        
        2. ТЕХНИЧЕСКИЙ КОНТЕКСТ:
           - Сохраняй все упомянутые технические термины, API, фреймворки, библиотеки
           - Кратко описывай ключевые технические проблемы и их решения
           - Сохраняй важные числа, версии, даты и имена
           - Включай названия функций, классов и методов, которые обсуждались
           - Используй полные названия технологий при первом упоминании
        
        3. КОД И ПРИМЕРЫ:
           - Сохраняй общую идею и цель примеров кода, а не полный код
           - Для важных фрагментов кода, опиши их функциональность и ключевые особенности
           - Отмечай ошибки в коде и способы их исправления, если они обсуждались
        
        4. СТИЛЬ И ФОРМАТ:
           - Используй сжатые, но полные предложения
           - НЕ ИСПОЛЬЗУЙ: "пользователь спросил", "ассистент ответил", любые метаописания
           - ФОРМАТ: "КОНТЕКСТ ДИАЛОГА: [информативное резюме с сохранением последовательности тем]"
           - Избегай повторений и избыточности
        
        МЕТРИКИ УСПЕХА:
        - Эффективное сокращение диалога при сохранении технического контекста
        - Возможность продолжить диалог на основе только суммаризации
        - Сохранение логической структуры и последовательности тем
        
        Пример хорошей суммаризации: 
        "КОНТЕКСТ ДИАЛОГА: Обсуждение оптимизации производительности Android-приложения. Идентифицированы проблемы: утечки памяти при использовании ViewModels (решение: правильное управление lifecycle), медленная загрузка изображений (решение: использование кэширования и библиотеки Coil). Рассмотрена архитектура MVVM для рефакторинга текущего кода. Текущий прогресс: оптимизирована основная Activity, выполнен профайлинг с Systrace (выявлены проблемные блоки кода), начато внедрение Kotlin Coroutines для асинхронных операций."
    """.trimIndent()
    
    /**
     * Создает суммаризацию диалога на основе списка сообщений
     */
    override suspend fun createSummary(
        messages: List<LLMMessage>,
        previousSummary: String?
    ): Result<Pair<String, DialogSummaryMetrics>> {
        if (messages.isEmpty()) {
            return Result.failure(IllegalStateException("Empty messages for summarization"))
        }
        
        return safeApiCall(logger) {
            logger.d("Создаем суммаризацию диалога из ${messages.size} сообщений")
            
            // Получаем настройки провайдера
            val providerSettings = providerSettingsRepository.getSettings()
            
            // Проверяем, что настройки модели валидны
            if (providerSettings.model.isBlank()) {
                throw IllegalStateException("Model name is empty")
            }
            
            if (providerSettings.apiUrl.isBlank()) {
                throw IllegalStateException("API URL is empty")
            }
            
            // Создаем системное сообщение
            val systemMessage = LLMMessage(
                role = MessageRole.SYSTEM,
                content = summarySystemPrompt
            )
            
            // Добавляем метаинформацию о требуемом коэффициенте сжатия и предыдущую суммаризацию
            val contextInstruction = if (previousSummary != null) {
                // Если есть предыдущая суммаризация, включаем ее в инструкцию
                """
                ВАЖНО: Учти предыдущую суммаризацию диалога:
                $previousSummary
                
                Объедини её с суммаризацией новых сообщений, создав единый полный контекст.
                Сожми следующие ${messages.size} сообщений, сохраняя их ключевую информацию.
                """
            } else {
                // Если предыдущей суммаризации нет, просто просим сжать диалог
                "Сожми следующий диалог, сохраняя их ключевую информацию. Текущее количество сообщений: ${messages.size}."
            }
            
            val userInstructionMessage = LLMMessage(
                role = MessageRole.USER,
                content = contextInstruction
            )
            
            // Формируем список сообщений: система + инструкция + сообщения для суммаризации
            val messagesWithSystem = listOf(systemMessage, userInstructionMessage) + messages
            
            // Замеряем время начала запроса
            val startTime = System.currentTimeMillis()
            
            // Создаем запрос
            val request = LLMChatRequestDTO(
                model = providerSettings.model,
                messages = messagesWithSystem.map { message ->
                    ChatMessageDTO(
                        role = message.role.key,
                        content = message.content
                    )
                },
                temperature = 0.0, // Низкая температура для стабильных и последовательных результатов
                maxTokens = 512, // Увеличиваем лимит токенов для более информативной суммаризации
                apiKey = providerSettings.apiKey,
                apiUrl = providerSettings.apiUrl
            )
            
            // Отправляем запрос к API
            val completionResponse = openAIApi.sendRequest(request)
            
            // Вычисляем время выполнения запроса
            val responseTime = System.currentTimeMillis() - startTime
            
            // Получаем содержимое сообщения
            val summaryContent = completionResponse.choices.firstOrNull()?.message?.content
                ?: throw IllegalStateException("Empty summary response")
            
            logger.d("Получена суммаризация: ${summaryContent.take(50)}${if (summaryContent.length > 50) "..." else ""}")
            
            // Создаем метрики
            val metrics = DialogSummaryMetrics(
                promptTokens = completionResponse.usage?.promptTokens ?: 0,
                completionTokens = completionResponse.usage?.completionTokens ?: 0,
                totalTokens = completionResponse.usage?.totalTokens ?: 0,
                responseTimeMs = responseTime
            )
            
            logger.d("Метрики суммаризации: $metrics")
            
            // Возвращаем суммаризацию и метрики
            Pair(summaryContent, metrics)
        }
    }
}
