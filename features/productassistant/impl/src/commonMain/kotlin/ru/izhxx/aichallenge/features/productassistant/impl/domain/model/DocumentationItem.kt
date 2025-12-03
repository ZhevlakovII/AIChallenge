package ru.izhxx.aichallenge.features.productassistant.impl.domain.model

/**
 * Domain model for a documentation item from FAQ
 */
data class DocumentationItem(
    val question: String,
    val answer: String,
    val category: DocumentationCategory,
    val keywords: List<String>,
    val relevanceScore: Double = 0.0
)

/**
 * Category of documentation
 */
enum class DocumentationCategory {
    CHAT_FUNCTIONALITY,
    LLM_SETTINGS,
    COMMON_ISSUES,
    TROUBLESHOOTING,
    GENERAL;

    fun toDisplayString(): String {
        return when (this) {
            CHAT_FUNCTIONALITY -> "Функциональность чата"
            LLM_SETTINGS -> "Настройки LLM"
            COMMON_ISSUES -> "Типичные проблемы"
            TROUBLESHOOTING -> "Решение проблем"
            GENERAL -> "Общее"
        }
    }
}
