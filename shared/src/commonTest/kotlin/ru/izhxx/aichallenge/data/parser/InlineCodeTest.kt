package ru.izhxx.aichallenge.data.parser

import ru.izhxx.aichallenge.domain.model.MarkdownElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InlineCodeTest {
    
    @Test
    fun testInlineCodeWithSurroundingText() {
        val input = "Activity наследуется от класса `android.app.Activity` и имеет собственный жизненный цикл."
        
        val result = MarkdownParser.parse(input)
        assertEquals(1, result.size)
        assertTrue(result[0] is MarkdownElement.Paragraph)
        
        val paragraph = result[0] as MarkdownElement.Paragraph
        // Проверяем, что параграф содержит 3 элемента: Text, InlineCode, Text
        assertEquals(3, paragraph.items.size)
        
        // Проверяем первый текстовый элемент
        assertTrue(paragraph.items[0] is MarkdownElement.Text)
        assertEquals("Activity наследуется от класса ", (paragraph.items[0] as MarkdownElement.Text).content)
        
        // Проверяем элемент с кодом
        assertTrue(paragraph.items[1] is MarkdownElement.InlineCode)
        assertEquals("android.app.Activity", (paragraph.items[1] as MarkdownElement.InlineCode).content)
        
        // Проверяем второй текстовый элемент (текст после кода)
        assertTrue(paragraph.items[2] is MarkdownElement.Text)
        assertEquals(" и имеет собственный жизненный цикл.", (paragraph.items[2] as MarkdownElement.Text).content)
    }
    
    @Test
    fun testMultipleInlineCodeInOneLine() {
        val input = "Activity объявляется в манифесте приложения (`AndroidManifest.xml`) и запускается через `Intent`."
        
        val result = MarkdownParser.parse(input)
        assertEquals(1, result.size)
        assertTrue(result[0] is MarkdownElement.Paragraph)
        
        val paragraph = result[0] as MarkdownElement.Paragraph
        // Проверяем, что параграф содержит 5 элементов: Text, InlineCode, Text, InlineCode, Text
        assertEquals(5, paragraph.items.size)
        
        // Проверяем элементы с кодом
        assertTrue(paragraph.items[1] is MarkdownElement.InlineCode)
        assertEquals("AndroidManifest.xml", (paragraph.items[1] as MarkdownElement.InlineCode).content)
        
        assertTrue(paragraph.items[3] is MarkdownElement.InlineCode)
        assertEquals("Intent", (paragraph.items[3] as MarkdownElement.InlineCode).content)
    }
}
