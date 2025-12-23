package ru.izhxx.aichallenge.tools.llm.config.model

import ru.izhxx.aichallenge.core.url.Url

/**
 * Настройки провайдера LLM API.
 * Содержит параметры для подключения к LLM провайдеру.
 *
 * @property apiUrl URL API сервиса LLM.
 * @property model Код модели LLM в сервисе.
 * @property apiKey Ключ для доступа к сервису LLM.
 */
class ProviderConfig(
    val apiUrl: Url,
    val model: String,
    val apiKey: String,
)