package ru.izhxx.aichallenge.domain.model.github

/**
 * Доменная модель GitHub-репозитория.
 *
 * Модель используется в слоях Domain/Presentation и не зависит от DTO.
 *
 * @property id Уникальный идентификатор репозитория
 * @property name Название репозитория
 * @property description Краткое описание (может отсутствовать)
 * @property htmlUrl Прямая ссылка на репозиторий в GitHub
 * @property stargazers Количество звёзд
 * @property language Основной язык репозитория (может отсутствовать)
 * @property updatedAt Дата последнего обновления (ISO-8601 строка)
 */
data class Repo(
    val id: Long,
    val name: String,
    val description: String?,
    val htmlUrl: String,
    val stargazers: Int,
    val language: String?,
    val updatedAt: String
)
