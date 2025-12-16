package ru.izhxx.aichallenge.core.foundation.strings

/**
 * Добавляет пробел в конец StringBuilder.
 *
 * Назначение:
 * - Упрощает построение строк с пробелами между элементами.
 * - Улучшает читаемость кода за счет явного указания намерения.
 *
 * Пример:
 * ```kotlin
 * val builder = StringBuilder()
 * builder.append("Hello").appendSpace().append("World")
 * // Результат: "Hello World"
 * ```
 *
 * @return StringBuilder с добавленным пробелом для цепочечных вызовов.
 */
fun StringBuilder.appendSpace(): StringBuilder = append(" ")
