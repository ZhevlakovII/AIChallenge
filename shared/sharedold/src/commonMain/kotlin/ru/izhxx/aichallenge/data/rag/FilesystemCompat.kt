package ru.izhxx.aichallenge.data.rag

/**
 * Платформенная обертка для чтения файла по абсолютному пути.
 * Возвращает текст файла в UTF-8 или null при ошибке/отсутствии.
 */
expect fun readFileTextCompat(path: String): String?
