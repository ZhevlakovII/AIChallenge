@file:OptIn(ExperimentalTime::class)

package ru.izhxx.aichallenge.features.productassistant.impl.data.repository

import kotlinx.datetime.Instant
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import ru.izhxx.aichallenge.features.productassistant.impl.data.datasource.LlmAnswerDataSource
import ru.izhxx.aichallenge.features.productassistant.impl.data.datasource.RagSearchDataSource
import ru.izhxx.aichallenge.features.productassistant.impl.data.datasource.TicketMcpDataSource
import ru.izhxx.aichallenge.features.productassistant.impl.domain.model.AssistantMode
import ru.izhxx.aichallenge.features.productassistant.impl.domain.model.AssistantQuery
import ru.izhxx.aichallenge.features.productassistant.impl.domain.model.AssistantResponse
import ru.izhxx.aichallenge.features.productassistant.impl.domain.model.DocumentationItem
import ru.izhxx.aichallenge.features.productassistant.impl.domain.model.ResponseSource
import ru.izhxx.aichallenge.features.productassistant.impl.domain.model.SourceType
import ru.izhxx.aichallenge.features.productassistant.impl.domain.model.SupportTicket
import ru.izhxx.aichallenge.features.productassistant.impl.domain.model.TicketComment
import ru.izhxx.aichallenge.features.productassistant.impl.domain.model.TicketStatus
import ru.izhxx.aichallenge.features.productassistant.impl.domain.repository.ProductAssistantRepository
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Implementation of ProductAssistantRepository
 */
class ProductAssistantRepositoryImpl(
    private val ragSearchDataSource: RagSearchDataSource,
    private val ticketMcpDataSource: TicketMcpDataSource,
    private val llmAnswerDataSource: LlmAnswerDataSource
) : ProductAssistantRepository {

    override suspend fun searchFaq(
        query: String,
        maxResults: Int
    ): Result<List<DocumentationItem>> {
        return runCatching {
            ragSearchDataSource.searchDocumentation(query, maxResults).getOrDefault(emptyList())
        }
    }

    override suspend fun getAllTickets(
        statusFilter: String?,
        tagFilter: String?
    ): Result<List<SupportTicket>> {
        return runCatching {
            val result = ticketMcpDataSource.listTickets(statusFilter, tagFilter).getOrThrow()
            val ticketsArray = result["tickets"]?.jsonArray
                ?: throw IllegalStateException("Invalid MCP response: missing 'tickets' array")

            ticketsArray.map { ticketElement ->
                val ticketObj = ticketElement.jsonObject
                val commentsArray = ticketObj["comments"]?.jsonArray ?: emptyList()
                val comments = commentsArray.map { commentElement ->
                    val commentObj = commentElement.jsonObject
                    TicketComment(
                        id = commentObj["id"]?.jsonPrimitive?.content ?: "",
                        authorId = commentObj["authorId"]?.jsonPrimitive?.content ?: "",
                        authorName = commentObj["authorName"]?.jsonPrimitive?.content ?: "",
                        content = commentObj["content"]?.jsonPrimitive?.content ?: "",
                        createdAt = Instant.parse(commentObj["createdAt"]?.jsonPrimitive?.content ?: Clock.System.now().toString()),
                        isInternal = commentObj["isInternal"]?.jsonPrimitive?.content?.toBoolean() ?: false
                    )
                }

                SupportTicket(
                    id = ticketObj["id"]?.jsonPrimitive?.content ?: "",
                    userId = ticketObj["userId"]?.jsonPrimitive?.content ?: "",
                    title = ticketObj["title"]?.jsonPrimitive?.content ?: "",
                    description = ticketObj["description"]?.jsonPrimitive?.content ?: "",
                    status = TicketStatus.fromString(ticketObj["status"]?.jsonPrimitive?.content ?: "open"),
                    createdAt = Instant.parse(ticketObj["createdAt"]?.jsonPrimitive?.content ?: ""),
                    updatedAt = Instant.parse(ticketObj["updatedAt"]?.jsonPrimitive?.content ?: Clock.System.now().toString()),
                    tags = ticketObj["tags"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
                    comments = comments
                )
            }
        }
    }

    override suspend fun getTicketById(ticketId: String): Result<SupportTicket?> {
        return runCatching {
            val result = ticketMcpDataSource.getTicket(ticketId).getOrNull()
                ?: return@runCatching null

            val commentsArray = result["comments"]?.jsonArray ?: emptyList()
            val comments = commentsArray.map { commentElement ->
                val commentObj = commentElement.jsonObject
                TicketComment(
                    id = commentObj["id"]?.jsonPrimitive?.content ?: "",
                    authorId = commentObj["authorId"]?.jsonPrimitive?.content ?: "",
                    authorName = commentObj["authorName"]?.jsonPrimitive?.content ?: "",
                    content = commentObj["content"]?.jsonPrimitive?.content ?: "",
                    createdAt = Instant.parse(commentObj["createdAt"]?.jsonPrimitive?.content ?: Clock.System.now().toString()),
                    isInternal = commentObj["isInternal"]?.jsonPrimitive?.content?.toBoolean() ?: false
                )
            }

            SupportTicket(
                id = result["id"]?.jsonPrimitive?.content ?: "",
                userId = result["userId"]?.jsonPrimitive?.content ?: "",
                title = result["title"]?.jsonPrimitive?.content ?: "",
                description = result["description"]?.jsonPrimitive?.content ?: "",
                status = TicketStatus.fromString(result["status"]?.jsonPrimitive?.content ?: "open"),
                createdAt = Instant.parse(result["createdAt"]?.jsonPrimitive?.content ?: ""),
                updatedAt = Instant.parse(result["updatedAt"]?.jsonPrimitive?.content ?: Clock.System.now().toString()),
                tags = result["tags"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
                comments = comments
            )
        }
    }

    override suspend fun searchTickets(
        query: String,
        maxResults: Int
    ): Result<List<SupportTicket>> {
        return runCatching {
            // Get all tickets and filter by query relevance
            val allTickets = getAllTickets().getOrThrow()

            val queryLower = query.lowercase()
            val queryWords = queryLower.split(Regex("\\s+")).filter { it.length > 2 }

            // Score tickets by relevance
            val scored = allTickets.map { ticket ->
                val score = calculateTicketRelevance(ticket, queryLower, queryWords)
                ticket to score
            }

            // Sort by score and take top results
            scored
                .filter { it.second > 0.0 }
                .sortedByDescending { it.second }
                .take(maxResults)
                .map { it.first }
        }
    }

    override suspend fun generateAnswer(
        query: AssistantQuery,
        faqContext: List<DocumentationItem>,
        ticketContext: List<SupportTicket>
    ): Result<AssistantResponse> {
        return runCatching {
            val systemPrompt = buildSystemPrompt(query.mode)
            val userPrompt = buildUserPrompt(query, faqContext, ticketContext)

            val llmAnswer = llmAnswerDataSource.generateAnswer(systemPrompt, userPrompt).getOrThrow()

            // Build response sources
            val sources = buildList {
                faqContext.forEach { faq ->
                    add(ResponseSource(
                        type = SourceType.FAQ,
                        reference = faq.question,
                        excerpt = faq.answer.take(200)
                    ))
                }
                ticketContext.forEach { ticket ->
                    add(ResponseSource(
                        type = SourceType.TICKET,
                        reference = "Тикет #${ticket.id.take(8)}",
                        excerpt = ticket.title
                    ))
                }
            }

            AssistantResponse(
                answer = llmAnswer,
                mode = query.mode,
                relatedTickets = ticketContext,
                relatedDocumentation = faqContext,
                confidence = calculateConfidence(faqContext, ticketContext),
                sources = sources
            )
        }
    }

    private fun buildSystemPrompt(mode: AssistantMode): String {
        return when (mode) {
            AssistantMode.FAQ_ONLY -> """
                Ты - Product Assistant для приложения AI Challenge. Твоя задача - отвечать на вопросы пользователей о продукте, используя предоставленную документацию и FAQ.

                Инструкции:
                1. Используй только информацию из предоставленного контекста FAQ
                2. Отвечай четко и по существу на русском языке
                3. Если в FAQ нет ответа на вопрос, честно скажи об этом
                4. Приводи конкретные примеры из документации
                5. Структурируй ответ: краткий ответ, затем детали
            """.trimIndent()

            AssistantMode.TICKET_ANALYSIS -> """
                Ты - Product Assistant для приложения AI Challenge. Твоя задача - анализировать тикеты поддержки и помогать пользователям решить проблемы.

                Инструкции:
                1. Проанализируй предоставленные тикеты поддержки
                2. Определи возможные причины проблемы
                3. Предложи конкретные шаги для решения
                4. Если видишь похожие проблемы в других тикетах, упомяни это
                5. Отвечай на русском языке, структурированно
            """.trimIndent()

            AssistantMode.FULL -> """
                Ты - Product Assistant для приложения AI Challenge. Твоя задача - давать комплексные ответы, используя как документацию, так и данные из тикетов поддержки.

                Инструкции:
                1. Используй информацию из FAQ и тикетов поддержки
                2. Сначала проверь, есть ли похожие проблемы в тикетах
                3. Дополни ответ информацией из документации
                4. Предложи конкретные решения на основе обоих источников
                5. Если проблема встречается часто (много тикетов), упомяни это
                6. Отвечай на русском языке, четко структурируй информацию
            """.trimIndent()
        }
    }

    private fun buildUserPrompt(
        query: AssistantQuery,
        faqContext: List<DocumentationItem>,
        ticketContext: List<SupportTicket>
    ): String {
        val prompt = StringBuilder()

        prompt.appendLine("Вопрос пользователя: ${query.text}")
        prompt.appendLine()

        if (faqContext.isNotEmpty()) {
            prompt.appendLine("=== Релевантная информация из FAQ ===")
            faqContext.forEachIndexed { index, faq ->
                prompt.appendLine("${index + 1}. ${faq.question}")
                prompt.appendLine("   ${faq.answer}")
                prompt.appendLine()
            }
        }

        if (ticketContext.isNotEmpty()) {
            prompt.appendLine("=== Релевантные тикеты поддержки ===")
            ticketContext.forEachIndexed { index, ticket ->
                prompt.appendLine("${index + 1}. Тикет #${ticket.id.take(8)} [${ticket.status.toDisplayString()}]")
                prompt.appendLine("   Заголовок: ${ticket.title}")
                prompt.appendLine("   Описание: ${ticket.description}")
                prompt.appendLine("   Теги: ${ticket.tags.joinToString(", ")}")
                prompt.appendLine()
            }
        }

        if (faqContext.isEmpty() && ticketContext.isEmpty()) {
            prompt.appendLine("(Релевантной информации в базе знаний не найдено)")
        }

        prompt.appendLine("Пожалуйста, предоставь полный и точный ответ на вопрос пользователя.")

        return prompt.toString()
    }

    private fun calculateTicketRelevance(
        ticket: SupportTicket,
        queryLower: String,
        queryWords: List<String>
    ): Double {
        var score = 0.0

        // Exact match in title
        if (ticket.title.lowercase().contains(queryLower)) {
            score += 10.0
        }

        // Exact match in description
        if (ticket.description.lowercase().contains(queryLower)) {
            score += 5.0
        }

        // Tag matches
        queryWords.forEach { word ->
            if (ticket.tags.any { it.lowercase().contains(word) }) {
                score += 3.0
            }
        }

        // Word matches in title
        queryWords.forEach { word ->
            if (ticket.title.lowercase().contains(word)) {
                score += 1.5
            }
        }

        // Word matches in description
        queryWords.forEach { word ->
            if (ticket.description.lowercase().contains(word)) {
                score += 0.5
            }
        }

        // Boost open tickets
        if (ticket.status == TicketStatus.OPEN) {
            score *= 1.2
        }

        return score
    }

    private fun calculateConfidence(
        faqContext: List<DocumentationItem>,
        ticketContext: List<SupportTicket>
    ): Double {
        var confidence = 0.0

        // Base confidence from FAQ
        if (faqContext.isNotEmpty()) {
            val avgFaqScore = faqContext.map { it.relevanceScore }.average()
            confidence += (avgFaqScore / 10.0) * 0.5 // Max 0.5 from FAQ
        }

        // Base confidence from tickets
        if (ticketContext.isNotEmpty()) {
            confidence += 0.3 // Having relevant tickets adds confidence
        }

        // Bonus for multiple sources
        if (faqContext.isNotEmpty() && ticketContext.isNotEmpty()) {
            confidence += 0.2
        }

        return confidence.coerceIn(0.0, 1.0)
    }

    override suspend fun createTicket(ticket: SupportTicket): Result<SupportTicket> {
        return runCatching {
            val result = ticketMcpDataSource.createTicket(
                title = ticket.title,
                description = ticket.description,
                tags = ticket.tags
            ).getOrThrow()

            SupportTicket(
                id = result["id"]?.jsonPrimitive?.content.orEmpty(),
                userId = ticket.userId,
                title = result["title"]?.jsonPrimitive?.content ?: ticket.title,
                description = result["description"]?.jsonPrimitive?.content ?: ticket.description,
                status = TicketStatus.fromString(result["status"]?.jsonPrimitive?.content ?: TicketStatus.OPEN.name),
                createdAt = Instant.parse(result["createdAt"]?.jsonPrimitive?.content ?: Clock.System.now().toString()),
                updatedAt = Instant.parse(result["updatedAt"]?.jsonPrimitive?.content ?: Clock.System.now().toString()),
                tags = result["tags"]?.jsonArray?.map { it.jsonPrimitive.content } ?: ticket.tags
            )
        }
    }

    override suspend fun updateTicketStatus(ticketId: String, newStatus: TicketStatus): Result<Unit> {
        return runCatching {
            ticketMcpDataSource.updateTicket(
                ticketId = ticketId,
                newStatus = newStatus.name.lowercase()
            ).getOrThrow()
            Unit
        }
    }

    override suspend fun updateTicketComment(ticketId: String, comment: String): Result<Unit> {
        return runCatching {
            ticketMcpDataSource.updateTicket(
                ticketId = ticketId,
                comment = comment
            ).getOrThrow()
            Unit
        }
    }

    override suspend fun getTicket(ticketId: String): Result<SupportTicket> {
        return getTicketById(ticketId).map { ticket ->
            ticket ?: throw IllegalStateException("Ticket not found: $ticketId")
        }
    }
}
