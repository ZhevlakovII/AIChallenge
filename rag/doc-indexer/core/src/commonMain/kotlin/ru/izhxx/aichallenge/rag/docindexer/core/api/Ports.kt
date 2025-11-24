package ru.izhxx.aichallenge.rag.docindexer.core.api

import ru.izhxx.aichallenge.rag.docindexer.core.model.DocumentIndex
import ru.izhxx.aichallenge.rag.docindexer.core.model.FileEntry

/**
 * Интерфейс чанкера текста.
 * Работает в символах; длины (maxChars/overlapChars) вычисляются снаружи из токенов.
 */
interface TextChunker {
    /**
     * Разбивает текст на диапазоны [start, end) с перекрытием.
     * @param text исходный текст
     * @param maxChars целевая длина чанка в символах
     * @param overlapChars перекрытие между чанками в символах
     * @return список диапазонов в исходном тексте
     */
    fun split(text: String, maxChars: Int, overlapChars: Int): List<IntRange>
}

/**
 * Интерфейс эмбеддера.
 * Возвращает вектор эмбеддинга для заданного текста.
 */
interface Embedder {
    suspend fun embed(text: String): List<Double>
}

/**
 * Интерфейс чтения контента из источника (JVM: файловая система).
 * Сканирует только .md файлы и читает их содержимое.
 */
interface ContentReader {
    fun scanMarkdownFiles(rootDir: String): List<FileEntry>
    fun read(entry: FileEntry): String
}

/**
 * Интерфейс хэширования (для получения sha256).
 */
interface Hasher {
    fun sha256(text: String): String
}

/**
 * Интерфейс записи индекса (JVM: в JSON-файл).
 */
interface IndexWriter {
    fun write(index: DocumentIndex, outputPath: String)
}
