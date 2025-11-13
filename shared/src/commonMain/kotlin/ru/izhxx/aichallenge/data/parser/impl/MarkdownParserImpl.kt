package ru.izhxx.aichallenge.data.parser.impl

import ru.izhxx.aichallenge.common.Logger
import ru.izhxx.aichallenge.data.parser.core.MarkdownParser
import ru.izhxx.aichallenge.domain.model.markdown.InlineElement
import ru.izhxx.aichallenge.domain.model.markdown.MarkdownNode

/**
 * Реализация парсера Markdown
 * Преобразует текст в формате Markdown в структуру MarkdownNode
 */
class MarkdownParserImpl : MarkdownParser {
    private val logger = Logger.forClass(this::class)

    // Предкомпилированные регулярные выражения для оптимизации производительности
    private val horizontalRuleRegex = Regex("^-{3,}$|^\\*{3,}$")
    private val headingRegex = Regex("^(#{1,6})\\s+(.+)$")
    private val unorderedListItemRegex = Regex("^\\s*-\\s+(.+)$")
    private val orderedListItemRegex = Regex("^\\s*\\d+\\.\\s+(.+)$")
    private val quoteRegex = Regex("^>\\s+(.+)$")

    /**
     * Парсит блоки Markdown и возвращает список узлов
     *
     * @param input текст в формате Markdown
     * @return результат с списком узлов Markdown
     */
    override fun parse(input: String): Result<List<MarkdownNode>> {
        return try {
            val result = parseMarkdown(input)
            Result.success(result)
        } catch (e: Exception) {
            logger.e("Ошибка при парсинге Markdown", e)
            Result.failure(e)
        }
    }

    /**
     * Основной метод парсинга Markdown
     */
    private fun parseMarkdown(text: String): List<MarkdownNode> {
        val nodes = mutableListOf<MarkdownNode>()
        val lines = text.split("\n")
        var i = 0

        while (i < lines.size) {
            val line = lines[i].trimEnd() // Убираем пробелы только в конце строки

            when {
                // Кодовый блок
                line.trimStart().startsWith("```") -> {
                    val result = parseCodeBlock(lines, i)
                    result.first?.let { nodes.add(it) }
                    i = result.second
                }

                // Заголовок
                line.matches(headingRegex) -> {
                    val match = headingRegex.find(line)!!
                    val level = match.groupValues[1].length
                    val content = match.groupValues[2]
                    val inlineElements = parseInlineElements(content)
                    nodes.add(MarkdownNode.Heading(level, inlineElements))
                }

                // Горизонтальная линия
                line.trim().matches(horizontalRuleRegex) -> {
                    nodes.add(MarkdownNode.HorizontalRule)
                }

                // Цитата
                line.matches(quoteRegex) -> {
                    val match = quoteRegex.find(line)!!
                    val content = match.groupValues[1]
                    val inlineElements = parseInlineElements(content)
                    nodes.add(MarkdownNode.Quote(inlineElements))
                }

                // Маркированный список
                line.matches(unorderedListItemRegex) -> {
                    val result = parseUnorderedList(lines, i)
                    nodes.add(result.first)
                    i = result.second
                }

                // Нумерованный список
                line.matches(orderedListItemRegex) -> {
                    val result = parseOrderedList(lines, i)
                    nodes.add(result.first)
                    i = result.second
                }

                // Пустые строки
                line.isBlank() -> {
                    nodes.add(MarkdownNode.Divider)
                }

                // Обычный текст/параграф
                else -> {
                    if (line.isNotBlank()) {
                        val inlineElements = parseInlineElements(line)
                        nodes.add(MarkdownNode.Paragraph(inlineElements))
                    }
                }
            }

            i++
        }

        return nodes
    }

    /**
     * Парсит кодовый блок, начиная с указанного индекса
     */
    private fun parseCodeBlock(
        lines: List<String>,
        startIndex: Int
    ): Pair<MarkdownNode.CodeBlock?, Int> {
        val startLine = lines[startIndex].trimStart()
        val language = startLine.removePrefix("```").trim()

        val codeLines = mutableListOf<String>()
        var i = startIndex + 1

        while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
            codeLines.add(lines[i])
            i++
        }

        // Если мы достигли конца текста без закрывающих ```, всё равно создаем блок кода
        if (i < lines.size) {
            i++ // Пропускаем закрывающий ```
        }

        return Pair(MarkdownNode.CodeBlock(codeLines.joinToString("\n"), language), i - 1)
    }

    /**
     * Парсит маркированный список, начиная с указанного индекса
     */
    private fun parseUnorderedList(
        lines: List<String>,
        startIndex: Int
    ): Pair<MarkdownNode.UnorderedList, Int> {
        val items = mutableListOf<MarkdownNode.ListItem>()
        var i = startIndex

        while (i < lines.size && lines[i].trimStart().matches(unorderedListItemRegex)) {
            val match = unorderedListItemRegex.find(lines[i].trimStart())!!
            val content = match.groupValues[1]
            val inlineElements = parseInlineElements(content)
            items.add(MarkdownNode.ListItem(inlineElements))
            i++
        }

        return Pair(MarkdownNode.UnorderedList(items), i - 1)
    }

    /**
     * Парсит нумерованный список, начиная с указанного индекса
     */
    private fun parseOrderedList(
        lines: List<String>,
        startIndex: Int
    ): Pair<MarkdownNode.OrderedList, Int> {
        val items = mutableListOf<MarkdownNode.ListItem>()
        var i = startIndex

        while (i < lines.size && lines[i].trimStart().matches(orderedListItemRegex)) {
            val match = orderedListItemRegex.find(lines[i].trimStart())!!
            val content = match.groupValues[1]
            val inlineElements = parseInlineElements(content)
            items.add(MarkdownNode.ListItem(inlineElements))
            i++
        }

        return Pair(MarkdownNode.OrderedList(items), i - 1)
    }

    /**
     * Парсит inline элементы в тексте: код в backticks, bold, italic, links
     * Используется оптимизированный алгоритм
     */
    override fun parseInlineElements(text: String): List<InlineElement> {
        val elements = mutableListOf<InlineElement>()
        var currentPos = 0

        while (currentPos < text.length) {
            // Ищем следующий inline элемент и обрабатываем их в порядке появления в тексте
            var nearestMatch: Triple<Int, String, Int>? = null // (position, type, endPosition)

            // Поиск [link](url)
            val linkMatch = findLink(text, currentPos)
            if (linkMatch != null) {
                nearestMatch = Triple(linkMatch.first, "link", linkMatch.second)
            }

            // Поиск **bold**
            val boldMatch = findBold(text, currentPos)
            if (boldMatch != null && (nearestMatch == null || boldMatch.first < nearestMatch.first)) {
                nearestMatch = Triple(boldMatch.first, "bold", boldMatch.second)
            }

            // Поиск *italic* (одинарная звезда, не часть **)
            val italicMatch = findItalic(text, currentPos)
            if (italicMatch != null && (nearestMatch == null || italicMatch.first < nearestMatch.first)) {
                nearestMatch = Triple(italicMatch.first, "italic", italicMatch.second)
            }

            // Поиск inline кода `code`
            val codeMatch = findInlineCode(text, currentPos)
            if (codeMatch != null && (nearestMatch == null || codeMatch.first < nearestMatch.first)) {
                nearestMatch = Triple(codeMatch.first, "code", codeMatch.second)
            }

            if (nearestMatch == null) {
                // Нет больше inline элементов, добавляем оставшийся текст
                if (currentPos < text.length) {
                    elements.add(InlineElement.Text(text.substring(currentPos)))
                }
                break
            }

            val (pos, type, endPos) = nearestMatch

            // Добавляем текст перед элементом
            if (pos > currentPos) {
                elements.add(InlineElement.Text(text.substring(currentPos, pos)))
            }

            // Обрабатываем найденный элемент, используя сохраненную в nearestMatch конечную позицию
            when (type) {
                "code" -> {
                    val code = text.substring(pos + 1, endPos - 1)
                    elements.add(InlineElement.Code(code))
                }

                "bold" -> {
                    val boldText = text.substring(pos + 2, endPos - 2)
                    // Рекурсивно парсим содержимое болда для поддержки вложенных элементов
                    val boldElements = parseInlineElements(boldText)
                    elements.add(InlineElement.Bold(boldElements))
                }

                "italic" -> {
                    val italicText = text.substring(pos + 1, endPos - 1)
                    // Рекурсивно парсим содержимое курсива для поддержки вложенных элементов
                    val italicElements = parseInlineElements(italicText)
                    elements.add(InlineElement.Italic(italicElements))
                }

                "link" -> {
                    val closeIndex = text.indexOf(']', pos)
                    val linkText = text.substring(pos + 1, closeIndex)
                    val url = text.substring(closeIndex + 2, endPos - 1)
                    elements.add(InlineElement.Link(linkText, url))
                }
            }

            currentPos = endPos
        }

        // Если элементов нет, просто возвращаем текст
        if (elements.isEmpty() && text.isNotBlank()) {
            elements.add(InlineElement.Text(text))
        }

        return elements
    }

    /**
     * Оптимизированный поиск инлайн-кода
     */
    private fun findInlineCode(text: String, startPos: Int): Pair<Int, Int>? {
        val open = text.indexOf('`', startPos)
        if (open != -1) {
            val close = text.indexOf('`', open + 1)
            if (close != -1) {
                return Pair(open, close + 1)
            }
        }
        return null
    }

    /**
     * Оптимизированный поиск жирного текста
     */
    private fun findBold(text: String, startPos: Int): Pair<Int, Int>? {
        val open = text.indexOf("**", startPos)
        if (open != -1) {
            val close = text.indexOf("**", open + 2)
            if (close != -1) {
                return Pair(open, close + 2)
            }
        }
        return null
    }

    /**
     * Оптимизированный поиск курсивного текста
     */
    private fun findItalic(text: String, startPos: Int): Pair<Int, Int>? {
        var pos = startPos
        while (true) {
            val open = text.indexOf('*', pos)
            if (open == -1) break

            // Проверяем, что это не часть **
            if (open + 1 < text.length && text[open + 1] == '*' ||
                open > 0 && text[open - 1] == '*'
            ) {
                pos = open + 1
                continue
            }

            var closePos = open + 1
            while (true) {
                val close = text.indexOf('*', closePos)
                if (close == -1) return null

                // Проверяем, что это не часть **
                if (close + 1 < text.length && text[close + 1] == '*' ||
                    close > 0 && text[close - 1] == '*'
                ) {
                    closePos = close + 1
                    continue
                }

                return Pair(open, close + 1)
            }
        }

        return null
    }

    /**
     * Оптимизированный поиск ссылок
     */
    private fun findLink(text: String, startPos: Int): Pair<Int, Int>? {
        val open = text.indexOf('[', startPos)
        if (open != -1) {
            val closeBracket = text.indexOf(']', open)
            if (closeBracket != -1 && closeBracket + 1 < text.length && text[closeBracket + 1] == '(') {
                val closeParen = text.indexOf(')', closeBracket)
                if (closeParen != -1) {
                    return Pair(open, closeParen + 1)
                }
            }
        }
        return null
    }
}
