//package ru.izhxx.aichallenge.data.parser
//
//import kotlin.test.Test
//import kotlin.test.assertEquals
//import kotlin.test.assertTrue
//
//class MarkdownParserTest {
//
//    @Test
//    fun testParseSimpleText() {
//        val result = MarkdownParser.parse("Hello World")
//        assertEquals(1, result.size)
//        assertTrue(result[0] is MarkdownElement.Paragraph)
//    }
//
//    @Test
//    fun testParseHeading() {
//        val result = MarkdownParser.parse("# Заголовок")
//        assertEquals(1, result.size)
//        assertTrue(result[0] is MarkdownElement.Heading)
//        val heading = result[0] as MarkdownElement.Heading
//        assertEquals(1, heading.level)
//        assertEquals("Заголовок", heading.content)
//    }
//
//    @Test
//    fun testParseCodeBlock() {
//        val code = """
//            ```kotlin
//            class MainActivity : AppCompatActivity() {
//                override fun onCreate(savedInstanceState: Bundle?) {
//                    super.onCreate(savedInstanceState)
//                }
//            }
//            ```
//        """.trimIndent()
//
//        val result = MarkdownParser.parse(code)
//        assertEquals(1, result.size)
//        assertTrue(result[0] is MarkdownElement.CodeBlock)
//        val codeBlock = result[0] as MarkdownElement.CodeBlock
//        assertEquals("kotlin", codeBlock.language)
//        assertTrue(codeBlock.code.contains("MainActivity"))
//    }
//
//    @Test
//    fun testParseInlineCode() {
//        val result = MarkdownParser.parse("Файл разметки `activity_main.xml`:")
//        assertEquals(1, result.size)
//        assertTrue(result[0] is MarkdownElement.Paragraph)
//        val paragraph = result[0] as MarkdownElement.Paragraph
//
//        // Должны быть элементы: Text, InlineCode, Text
//        assertTrue(paragraph.items.any { it is MarkdownElement.InlineCode })
//        val inlineCode = paragraph.items.filterIsInstance<MarkdownElement.InlineCode>().first()
//        assertEquals("activity_main.xml", inlineCode.content)
//    }
//
//    @Test
//    fun testParseInlineCodePreservesTextAfter() {
//        val result = MarkdownParser.parse("Код `example` и текст после")
//        assertEquals(1, result.size)
//        assertTrue(result[0] is MarkdownElement.Paragraph)
//        val paragraph = result[0] as MarkdownElement.Paragraph
//
//        // Должны быть элементы: Text "Код ", InlineCode "example", Text " и текст после"
//        val textElements = paragraph.items.filterIsInstance<MarkdownElement.Text>()
//        val codeElements = paragraph.items.filterIsInstance<MarkdownElement.InlineCode>()
//
//        assertEquals(2, textElements.size)
//        assertEquals(1, codeElements.size)
//        assertEquals("example", codeElements.first().content)
//        assertTrue(textElements.any { it.content.contains("и текст после") })
//    }
//
//    @Test
//    fun testParseBold() {
//        val result = MarkdownParser.parse("Это **жирный** текст")
//        assertEquals(1, result.size)
//        assertTrue(result[0] is MarkdownElement.Paragraph)
//        val paragraph = result[0] as MarkdownElement.Paragraph
//
//        val boldElements = paragraph.items.filterIsInstance<MarkdownElement.Bold>()
//        assertEquals(1, boldElements.size)
//        assertEquals("жирный", boldElements.first().content)
//    }
//
//    @Test
//    fun testParseItalic() {
//        val result = MarkdownParser.parse("Это *курсивный* текст")
//        assertEquals(1, result.size)
//        assertTrue(result[0] is MarkdownElement.Paragraph)
//        val paragraph = result[0] as MarkdownElement.Paragraph
//
//        val italicElements = paragraph.items.filterIsInstance<MarkdownElement.Italic>()
//        assertEquals(1, italicElements.size)
//        assertEquals("курсивный", italicElements.first().content)
//    }
//
//    @Test
//    fun testParseLink() {
//        val result = MarkdownParser.parse("Посетите [сайт](https://example.com)")
//        assertEquals(1, result.size)
//        assertTrue(result[0] is MarkdownElement.Paragraph)
//        val paragraph = result[0] as MarkdownElement.Paragraph
//
//        val linkElements = paragraph.items.filterIsInstance<MarkdownElement.Link>()
//        assertEquals(1, linkElements.size)
//        val link = linkElements.first()
//        assertEquals("сайт", link.text)
//        assertEquals("https://example.com", link.url)
//    }
//
//    @Test
//    fun testParseQuote() {
//        val result = MarkdownParser.parse("> Это цитата")
//        assertEquals(1, result.size)
//        assertTrue(result[0] is MarkdownElement.Quote)
//        val quote = result[0] as MarkdownElement.Quote
//        assertEquals("Это цитата", quote.content)
//    }
//
//    @Test
//    fun testParseMixedInlineElements() {
//        val result = MarkdownParser.parse("Текст с **жирным** и *курсивом* и `кодом` и [ссылкой](http://test.com)")
//        assertEquals(1, result.size)
//        assertTrue(result[0] is MarkdownElement.Paragraph)
//        val paragraph = result[0] as MarkdownElement.Paragraph
//
//        val boldElements = paragraph.items.filterIsInstance<MarkdownElement.Bold>()
//        val italicElements = paragraph.items.filterIsInstance<MarkdownElement.Italic>()
//        val codeElements = paragraph.items.filterIsInstance<MarkdownElement.InlineCode>()
//        val linkElements = paragraph.items.filterIsInstance<MarkdownElement.Link>()
//
//        assertEquals(1, boldElements.size)
//        assertEquals(1, italicElements.size)
//        assertEquals(1, codeElements.size)
//        assertEquals(1, linkElements.size)
//    }
//
//    @Test
//    fun testParseOrderedList() {
//        val text = """
//            1. Первый
//            2. Второй
//            3. Третий
//        """.trimIndent()
//
//        val result = MarkdownParser.parse(text)
//        assertEquals(1, result.size)
//        assertTrue(result[0] is MarkdownElement.OrderedList)
//        val list = result[0] as MarkdownElement.OrderedList
//        assertEquals(3, list.items.size)
//        assertEquals("Первый", list.items[0])
//        assertEquals("Второй", list.items[1])
//        assertEquals("Третий", list.items[2])
//    }
//
//    @Test
//    fun testParseComplexMarkdownWithCodeBlocksAndInlineCode() {
//        val markdown = """
//            Вот простой пример Activity в Android на языке Kotlin:
//
//            ```kotlin
//            class MainActivity : AppCompatActivity() {
//                override fun onCreate(savedInstanceState: Bundle?) {
//                    super.onCreate(savedInstanceState)
//                }
//            }
//            ```
//
//            Файл разметки `activity_main.xml`:
//
//            ```xml
//            <LinearLayout>
//                <TextView android:text="Hello" />
//            </LinearLayout>
//            ```
//
//            И не забудь объявить Activity в `AndroidManifest.xml`:
//
//            ```xml
//            <activity android:name=".MainActivity" />
//            ```
//
//            Этот пример создаёт экран с текстом и кнопкой.
//        """.trimIndent()
//
//        val result = MarkdownParser.parse(markdown)
//
//        // Проверяем, что никакой текст не потерян
//        assertTrue(result.isNotEmpty())
//
//        // Должны быть параграфы
//        val paragraphs = result.filterIsInstance<MarkdownElement.Paragraph>()
//        assertTrue(paragraphs.isNotEmpty())
//
//        // Должны быть кодовые блоки
//        val codeBlocks = result.filterIsInstance<MarkdownElement.CodeBlock>()
//        assertEquals(3, codeBlocks.size)
//
//        // Проверяем языки кодовых блоков
//        assertEquals("kotlin", codeBlocks[0].language)
//        assertEquals("xml", codeBlocks[1].language)
//        assertEquals("xml", codeBlocks[2].language)
//
//        // Проверяем что inline код сохранился
//        val allElements = result.flatMap {
//            if (it is MarkdownElement.Paragraph) it.items else emptyList()
//        }
//        val inlineCodeElements = allElements.filterIsInstance<MarkdownElement.InlineCode>()
//        assertTrue(inlineCodeElements.isNotEmpty())
//        assertTrue(inlineCodeElements.any { it.content == "activity_main.xml" })
//        assertTrue(inlineCodeElements.any { it.content == "AndroidManifest.xml" })
//
//        // Проверяем что текст после кодовых блоков сохранился
//        val lastParagraph = paragraphs.last()
//        val lastParagraphText = lastParagraph.items
//            .filterIsInstance<MarkdownElement.Text>()
//            .joinToString("") { it.content }
//        assertTrue(lastParagraphText.contains("Этот пример создаёт экран"))
//    }
//
//    @Test
//    fun testParseMultipleInlineCodeInOneLine() {
//        val result = MarkdownParser.parse("Используй `var1` или `var2` или `var3`")
//        assertEquals(1, result.size)
//        assertTrue(result[0] is MarkdownElement.Paragraph)
//        val paragraph = result[0] as MarkdownElement.Paragraph
//
//        val codeElements = paragraph.items.filterIsInstance<MarkdownElement.InlineCode>()
//        assertEquals(3, codeElements.size)
//        assertEquals("var1", codeElements[0].content)
//        assertEquals("var2", codeElements[1].content)
//        assertEquals("var3", codeElements[2].content)
//    }
//
//    @Test
//    fun testEmptyLines() {
//        val text = """
//            Первый параграф
//
//            Второй параграф
//        """.trimIndent()
//
//        val result = MarkdownParser.parse(text)
//        // Пустые строки пропускаются, поэтому должно быть 2 параграфа
//        val paragraphs = result.filterIsInstance<MarkdownElement.Paragraph>()
//        assertEquals(2, paragraphs.size)
//    }
//}
