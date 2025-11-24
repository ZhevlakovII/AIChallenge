package ru.izhxx.aichallenge.data.model.github

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.izhxx.aichallenge.domain.model.github.Repo

/**
 * DTO GitHub репозитория (Data-слой).
 * Все поля снабжены @SerialName для строгого соответствия JSON-ключам GitHub API.
 */
@Serializable
data class RepoDTO(
    @SerialName("id")
    val id: Long,
    @SerialName("name")
    val name: String,
    @SerialName("description")
    val description: String? = null,
    @SerialName("html_url")
    val htmlUrl: String,
    @SerialName("stargazers_count")
    val stargazersCount: Int = 0,
    @SerialName("language")
    val language: String? = null,
    @SerialName("updated_at")
    val updatedAt: String
)

/**
 * Преобразование DTO -> Domain-модель.
 */
fun RepoDTO.toDomain(): Repo = Repo(
    id = id,
    name = name,
    description = description,
    htmlUrl = htmlUrl,
    stargazers = stargazersCount,
    language = language,
    updatedAt = updatedAt
)
