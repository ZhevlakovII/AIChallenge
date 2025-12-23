package ru.izhxx.aichallenge.tools.embedder.core.chunks

enum class ChunkLevel {
    PARENT,     // Большой чанк для контекста
    CHILD       // Маленький чанк для точного поиска
}
