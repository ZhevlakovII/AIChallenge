package ru.izhxx.aichallenge.data.parser

import ru.izhxx.aichallenge.domain.model.MarkdownElement

/**
 * Парсер для преобразования markdown текста в структурированные элементы
 * Все преобразования выполняются в фоновом потоке до отображения на UI
 */
object MarkdownParser {

    fun parse(text: String): List<MarkdownElement> {
        val elements = mutableListOf<MarkdownElement>()
        val lines = text.split("\n")
        var i = 0

        while (i < lines.size) {
            val line = lines[i]

            when {
                // Кодовый блок - имеет приоритет
                line.trim().startsWith("```") -> {
                    val language = line.trim().removePrefix("```").trim()
                    val codeLines = mutableListOf<String>()
                    i++
                    while (i < lines.size && !lines[i].trim().startsWith("```")) {
                        codeLines.add(lines[i])
                        i++
                    }
                    elements.add(MarkdownElement.CodeBlock(codeLines.joinToString("\n"), language))
                }

                // Заголовки
                line.startsWith("#") && line.takeWhile { it == '#' }.length < 7 -> {
                    val level = line.takeWhile { it == '#' }.length
                    if (line.length > level && line[level] == ' ') {
                        val content = line.removePrefix("#".repeat(level)).trim()
                        if (content.isNotEmpty()) {
                            elements.add(MarkdownElement.Heading(level, content))
                        } else {
                            elements.add(MarkdownElement.Paragraph(listOf(MarkdownElement.Text(line))))
                        }
                    } else {
                        elements.add(MarkdownElement.Paragraph(listOf(MarkdownElement.Text(line))))
                    }
                }

                // Цитаты
                line.startsWith("> ") -> {
                    val content = line.removePrefix("> ").trim()
                    elements.add(MarkdownElement.Quote(content))
                }

                // Маркированные списки
                line.trim().startsWith("- ") && line.trim().length > 2 -> {
                    val items = mutableListOf<String>()
                    while (i < lines.size && lines[i].trim().startsWith("- ") && lines[i].trim().length > 2) {
                        items.add(lines[i].trim().removePrefix("- ").trim())
                        i++
                    }
                    i--
                    if (items.isNotEmpty()) {
                        elements.add(MarkdownElement.UnorderedList(items))
                    }
                }

                // Нумерованные списки
                line.trim().matches(Regex("^\\d+\\.\\s+.*")) -> {
                    val items = mutableListOf<String>()
                    while (i < lines.size && lines[i].trim().matches(Regex("^\\d+\\.\\s+.*"))) {
                        items.add(lines[i].trim().replaceFirst(Regex("^\\d+\\.\\s+"), ""))
                        i++
                    }
                    i--
                    if (items.isNotEmpty()) {
                        elements.add(MarkdownElement.OrderedList(items))
                    }
                }

                // Пустые строки
                line.isBlank() -> {
                    // Пропускаем
                }

                // Обычный текст/параграф
                else -> {
                    if (line.isNotBlank()) {
                        val inlineElements = parseInlineElements(line)
                        if (inlineElements.isNotEmpty()) {
                            elements.add(MarkdownElement.Paragraph(items = inlineElements))
                        }
                    }
                }
            }
            i++
        }

        return elements
    }

    /**
     * Парсит inline элементы в тексте: код в backticks, bold, italic, links
     * Обработка backticks имеет приоритет, чтобы не потерять содержимое после кодовых блоков
     */
    private fun parseInlineElements(text: String): List<MarkdownElement> {
        val elements = mutableListOf<MarkdownElement>()
        var currentPos = 0

        while (currentPos < text.length) {
            // Ищем следующий inline элемент и обрабатываем их в порядке появления в тексте
            var nearestMatch: Triple<Int, String, Int>? = null // (position, type, endPosition)

            // Поиск [link](url)
            var searchPos = currentPos
            while (searchPos < text.length) {
                val linkOpenIndex = text.indexOf('[', searchPos)
                if (linkOpenIndex == -1 || linkOpenIndex >= text.length) break

                val linkCloseIndex = text.indexOf(']', linkOpenIndex + 1)
                if (linkCloseIndex != -1 && linkCloseIndex + 1 < text.length && text[linkCloseIndex + 1] == '(') {
                    val urlCloseIndex = text.indexOf(')', linkCloseIndex + 2)
                    if (urlCloseIndex != -1) {
                        if (nearestMatch == null || linkOpenIndex < nearestMatch.first) {
                            nearestMatch = Triple(linkOpenIndex, "link", urlCloseIndex + 1)
                        }
                        break
                    }
                }
                searchPos = linkOpenIndex + 1
            }

            // Поиск **bold**
            val boldIndex = text.indexOf("**", currentPos)
            if (boldIndex != -1 && boldIndex < text.length - 1) {
                val closeIndex = text.indexOf("**", boldIndex + 2)
                if (closeIndex != -1) {
                    if (nearestMatch == null || boldIndex < nearestMatch.first) {
                        nearestMatch = Triple(boldIndex, "bold", closeIndex + 2)
                    }
                }
            }

            // Поиск *italic* (одинарная звезда, не часть **)
            val italicIndex = findItalicStart(text, currentPos)
            if (italicIndex != -1) {
                val closeIndex = findItalicEnd(text, italicIndex + 1)
                if (closeIndex != -1) {
                    if (nearestMatch == null || italicIndex < nearestMatch.first) {
                        nearestMatch = Triple(italicIndex, "italic", closeIndex + 1)
                    }
                }
            }

            // Поиск inline кода - обрабатываем его как и другие элементы в порядке появления
            val backtickIndex = text.indexOf('`', currentPos)
            if (backtickIndex != -1 && backtickIndex < text.length) {
                val closeIndex = text.indexOf('`', backtickIndex + 1)
                if (closeIndex != -1 && closeIndex < text.length) {
                    if (nearestMatch == null || backtickIndex < nearestMatch.first) {
                        nearestMatch = Triple(backtickIndex, "code", closeIndex + 1)
                    }
                } else {
                    // Если закрывающий backtick не найден, считаем что это одиночный символ
                    // и добавляем его как обычный текст
                    if (nearestMatch == null || backtickIndex < nearestMatch.first) {
                        // Добавляем символ ` как обычный текст
                        if (backtickIndex > currentPos) {
                            elements.add(MarkdownElement.Text(text.substring(currentPos, backtickIndex + 1)))
                            currentPos = backtickIndex + 1
                            continue
                        }
                    }
                }
            }

            if (nearestMatch == null) {
                // Нет больше inline элементов, добавляем оставшийся текст
                if (currentPos < text.length) {
                    elements.add(MarkdownElement.Text(text.substring(currentPos)))
                }
                break
            }

            val (pos, type, endPos) = nearestMatch

            // Добавляем текст перед элементом
            if (pos > currentPos) {
                elements.add(MarkdownElement.Text(text.substring(currentPos, pos)))
            }

            // Обрабатываем найденный элемент, используя сохраненную в nearestMatch конечную позицию
            when (type) {
                "code" -> {
                    // Используем endPos из nearestMatch вместо повторного поиска
                    val code = text.substring(pos + 1, endPos - 1)
                    elements.add(MarkdownElement.InlineCode(code))
                    currentPos = endPos
                }

                "bold" -> {
                    val bold = text.substring(pos + 2, endPos - 2)
                    elements.add(MarkdownElement.Bold(bold))
                    currentPos = endPos
                }

                "italic" -> {
                    val italic = text.substring(pos + 1, endPos - 1)
                    elements.add(MarkdownElement.Italic(italic))
                    currentPos = endPos
                }

                "link" -> {
                    val linkCloseIndex = text.indexOf(']', pos + 1)
                    val linkText = text.substring(pos + 1, linkCloseIndex)
                    val urlOpenIndex = linkCloseIndex + 1
                    val url = text.substring(urlOpenIndex + 1, endPos - 1)
                    elements.add(MarkdownElement.Link(linkText, url))
                    currentPos = endPos
                }
            }
        }

        // Если элементов нет, просто возвращаем текст
        if (elements.isEmpty()) {
            elements.add(MarkdownElement.Text(text))
        }

        return elements
    }

    /**
     * Находит начало italic (одинарная звезда, но не часть **)
     */
    private fun findItalicStart(text: String, startPos: Int): Int {
        var pos = startPos
        while (pos < text.length) {
            val index = text.indexOf('*', pos)
            if (index == -1) return -1

            // Проверяем, что это не часть **
            // Если следующий символ тоже *, значит это начало bold (**) - пропускаем
            if (index + 1 < text.length && text[index + 1] == '*') {
                pos = index + 2
                continue
            }
            
            // Если предыдущий символ тоже *, значит это конец bold (**) - пропускаем
            if (index > 0 && text[index - 1] == '*') {
                pos = index + 1
                continue
            }
            
            return index
        }
        return -1
    }

    /**
     * Находит конец italic (одинарная звезда, но не часть **)
     */
    private fun findItalicEnd(text: String, startPos: Int): Int {
        var pos = startPos
        while (pos < text.length) {
            val index = text.indexOf('*', pos)
            if (index == -1) return -1

            // Проверяем, что это не часть **
            // Если следующий символ тоже *, значит это начало bold (**) - пропускаем
            if (index + 1 < text.length && text[index + 1] == '*') {
                pos = index + 2
                continue
            }
            
            // Если предыдущий символ тоже *, значит это конец bold (**) - пропускаем
            if (index > 0 && text[index - 1] == '*') {
                pos = index + 1
                continue
            }
            
            return index
        }
        return -1
    }
}
