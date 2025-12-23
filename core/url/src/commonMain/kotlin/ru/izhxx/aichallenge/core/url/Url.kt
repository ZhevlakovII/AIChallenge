package ru.izhxx.aichallenge.core.url

import kotlin.jvm.JvmInline

/**
 * Value class для типобезопасного представления URL-адресов.
 *
 * Назначение:
 * - Предоставляет типобезопасную обертку над строковым URL.
 * - Использует value class (@JvmInline) для zero-cost абстракции (без overhead на boxing).
 * - Помогает избежать ошибок при работе с URL (передача вместо обычной строки).
 *
 * Особенности:
 * - Value class компилируется без создания объекта-обертки (inline на уровне байткода).
 * - Поддерживаются все типы URL (HTTP, HTTPS, FTP, файловые пути и т.д.).
 * - Не выполняет валидацию URL (это ответственность слоя использования).
 *
 * Правила использования:
 * - Используйте Url вместо String для параметров и полей, представляющих URL.
 * - Валидацию URL выполняйте до создания Url или при использовании.
 * - Для конвертации в строку используйте свойство [data].
 *
 * Пример:
 * ```kotlin
 * val apiUrl = Url("https://api.example.com/v1")
 * val fileUrl = Url("file:///path/to/file.txt")
 *
 * fun fetchData(url: Url) {
 *     httpClient.get(url.data)
 * }
 *
 * fetchData(apiUrl) // Типобезопасно
 * // fetchData("https://...") // Ошибка компиляции
 * ```
 *
 * @property data Строковое представление URL.
 */
@JvmInline
value class Url(val data: String)
