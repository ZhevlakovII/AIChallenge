package ru.izhxx.aichallenge.features.productassistant.impl.presentation.mapper

import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import ru.izhxx.aichallenge.features.productassistant.impl.domain.model.AssistantResponse
import ru.izhxx.aichallenge.features.productassistant.impl.domain.model.DocumentationItem
import ru.izhxx.aichallenge.features.productassistant.impl.domain.model.ResponseSource
import ru.izhxx.aichallenge.features.productassistant.impl.domain.model.SupportTicket
import ru.izhxx.aichallenge.features.productassistant.impl.domain.model.TicketStatus
import ru.izhxx.aichallenge.features.productassistant.impl.presentation.model.AssistantResponseUi
import ru.izhxx.aichallenge.features.productassistant.impl.presentation.model.FaqItemUi
import ru.izhxx.aichallenge.features.productassistant.impl.presentation.model.SourceUi
import ru.izhxx.aichallenge.features.productassistant.impl.presentation.model.TicketUi
import kotlin.time.ExperimentalTime

/**
 * Mapper for converting domain models to UI models
 */
class ProductAssistantUiMapper {

    @OptIn(ExperimentalTime::class)
    fun toUi(response: AssistantResponse): AssistantResponseUi {
        return AssistantResponseUi(
            answer = response.answer,
            mode = response.mode,
            relatedTickets = response.relatedTickets.map { toUi(it) },
            relatedDocumentation = response.relatedDocumentation.map { toUi(it) },
            confidence = response.confidence,
            sources = response.sources.map { toUi(it) }
        )
    }

    @OptIn(ExperimentalTime::class)
    fun toUi(ticket: SupportTicket): TicketUi {
        val (statusColor, statusText) = when (ticket.status) {
            TicketStatus.OPEN -> 0xFFE57373L to "Открыт"
            TicketStatus.IN_PROGRESS -> 0xFFFFB74DL to "В работе"
            TicketStatus.RESOLVED -> 0xFF81C784L to "Решён"
            TicketStatus.CLOSED -> 0xFF26EB9C to "Закрыт"
        }

        val dateTime = ticket.createdAt.toLocalDateTime(TimeZone.currentSystemDefault())
        val dateStr = "${dateTime.day}.${dateTime.month.number}.${dateTime.year}"

        return TicketUi(
            id = ticket.id,
            title = ticket.title,
            description = ticket.description,
            status = statusText,
            statusColor = statusColor,
            tags = ticket.tags,
            createdAt = dateStr
        )
    }

    fun toUi(faq: DocumentationItem): FaqItemUi {
        return FaqItemUi(
            question = faq.question,
            answer = faq.answer,
            category = faq.category.toDisplayString(),
            relevanceScore = faq.relevanceScore
        )
    }

    fun toUi(source: ResponseSource): SourceUi {
        return SourceUi(
            type = source.type.name,
            typeDisplayName = source.type.toDisplayString(),
            reference = source.reference,
            excerpt = source.excerpt
        )
    }
}
