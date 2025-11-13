//package ru.izhxx.aichallenge.data.parser
//
//import kotlin.test.Test
//import kotlin.test.assertEquals
//
///**
// * Тесты для воспроизведения и проверки проблемы, о которой сообщил пользователь
// */
//class UserReportedIssueTest {
//
//    @Test
//    fun testUserReportedIssue() {
//        // Это точно такой же текст, который был в сообщении пользователя
//        val input = """
//            Activity в Android — это компонент приложения, который представляет собой отдельный экран с пользовательским интерфейсом. Каждое окно, которое видит пользователь в приложении (например, экран входа, главный экран, настройки), обычно реализуется как отдельная Activity. Она отвечает за управление пользовательским интерфейсом, обработку взаимодействия пользователя и жизненный цикл экрана.
//
//            Activity наследуется от класса `android.app.Activity` и имеет собственный жизненный цикл, состоящий из методов, таких как `onCreate()`, `onStart()`, `onResume()`, `onPause()`, `onStop()`, `onDestroy()`. Эти методы вызываются системой при переходе Activity между состояниями (создание, запуск, приостановка, остановка, уничтожение).
//
//            Activity объявляется в манифесте приложения (`AndroidManifest.xml`) и может запускаться другими компонентами с помощью `Intent`.
//        """.trimIndent()
//
//        val result = MarkdownParser.parse(input)
//
//        // Проверяем, что результат содержит 3 параграфа
//        val paragraphs = result.filterIsInstance<MarkdownElement.Paragraph>()
//        assertEquals(3, paragraphs.size)
//
//        // Второй параграф должен содержать несколько InlineCode
//        val secondParagraph = paragraphs[1]
//        val inlineCodes = secondParagraph.items.filterIsInstance<MarkdownElement.InlineCode>()
//
//        // Выводим все найденные inline коды для анализа
//        println("Found ${inlineCodes.size} inline codes in second paragraph:")
//        inlineCodes.forEachIndexed { index, code ->
//            println("$index: '${code.content}'")
//        }
//
//        // Проверяем, что все inline коды правильно распарсены
//        // Ожидаем 7, потому что есть еще onDestroy()
//        assertEquals(7, inlineCodes.size)
//
//        // Проверяем конкретные значения
//        assertEquals("android.app.Activity", inlineCodes[0].content)
//        assertEquals("onCreate()", inlineCodes[1].content)
//        assertEquals("onStart()", inlineCodes[2].content)
//        assertEquals("onResume()", inlineCodes[3].content)
//        assertEquals("onPause()", inlineCodes[4].content)
//        assertEquals("onStop()", inlineCodes[5].content)
//        assertEquals("onDestroy()", inlineCodes[6].content)
//
//        // Третий параграф должен содержать 2 InlineCode
//        val thirdParagraph = paragraphs[2]
//        val thirdParaInlineCodes = thirdParagraph.items.filterIsInstance<MarkdownElement.InlineCode>()
//
//        assertEquals(2, thirdParaInlineCodes.size)
//        assertEquals("AndroidManifest.xml", thirdParaInlineCodes[0].content)
//        assertEquals("Intent", thirdParaInlineCodes[1].content)
//    }
//}
