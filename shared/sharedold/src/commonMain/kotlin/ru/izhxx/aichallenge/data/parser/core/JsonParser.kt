package ru.izhxx.aichallenge.data.parser.core

import ru.izhxx.aichallenge.domain.model.message.ParsedMessage

/**
 * Интерфейс для парсера JSON
 * Отвечает за преобразование JSON в структурированное сообщение
 */
interface JsonParser : Parser<String, ParsedMessage.Structured>
