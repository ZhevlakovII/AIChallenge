package ru.izhxx.aichallenge.data.parser.core

import ru.izhxx.aichallenge.domain.model.config.ResponseFormat
import ru.izhxx.aichallenge.domain.model.message.ParsedMessage

/**
 * Интерфейс для главного парсера результатов
 * Единая точка входа для парсинга всех типов ответов
 */
interface ResultParser : Parser<Pair<String, ResponseFormat>, ParsedMessage>
