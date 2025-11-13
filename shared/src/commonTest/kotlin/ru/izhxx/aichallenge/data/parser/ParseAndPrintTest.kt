//package ru.izhxx.aichallenge.data.parser
//
//import kotlin.test.Test
//
///**
// * Тест для разбора примера пользователя и вывода результатов
// */
//class ParseAndPrintTest {
//
//    @Test
//    fun testParseAndPrintUserExample() {
//        // Пример из сообщения пользователя
//        val input = """
//            Activity в Android — это компонент приложения, который представляет собой отдельный экран с пользовательским интерфейсом. Каждое окно, которое видит пользователь в приложении (например, экран входа, главный экран, настройки), обычно реализуется как отдельная Activity. Она отвечает за управление пользовательским интерфейсом, обработку взаимодействия пользователя и жизненный цикл экрана.
//
//            Activity наследуется от класса `android.app.Activity` и имеет собственный жизненный цикл, состоящий из методов, таких как `onCreate()`, `onStart()`, `onResume()`, `onPause()`, `onStop()`, `onDestroy()`. Эти методы вызываются системой при переходе Activity между состояниями (создание, запуск, приостановка, остановка, уничтожение).
//
//            Activity объявляется в манифесте приложения (`AndroidManifest.xml`) и может запускаться другими компонентами с помощью `Intent`.
//        """.trimIndent()
//
//        val elements = MarkdownParser.parse(input)
//
//        // Вывод результатов в консоль для анализа
//        println("\n-------- PARSED ELEMENTS --------")
//        elements.forEachIndexed { index, element ->
//            println("\nElement $index: ${element::class.simpleName}")
//
//            if (element is MarkdownElement.Paragraph) {
//                println("  Paragraph with ${element.items.size} items:")
//                element.items.forEachIndexed { itemIndex, item ->
//                    when (item) {
//                        is MarkdownElement.Text -> println("    $itemIndex: Text = '${item.content}'")
//                        is MarkdownElement.InlineCode -> println("    $itemIndex: InlineCode = '${item.content}'")
//                        is MarkdownElement.Bold -> println("    $itemIndex: Bold = '${item.content}'")
//                        is MarkdownElement.Italic -> println("    $itemIndex: Italic = '${item.content}'")
//                        is MarkdownElement.Link -> println("    $itemIndex: Link = '${item.text}' (${item.url})")
//                        else -> println("    $itemIndex: ${item::class.simpleName}")
//                    }
//                }
//            }
//        }
//        println("\n-------- END OF PARSED ELEMENTS --------")
//    }
//}
