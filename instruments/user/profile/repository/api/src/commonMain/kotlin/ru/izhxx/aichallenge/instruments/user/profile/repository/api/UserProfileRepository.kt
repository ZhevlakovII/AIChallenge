package ru.izhxx.aichallenge.instruments.user.profile.repository.api

import ru.izhxx.aichallenge.instruments.user.profile.model.UserProfile

/**
 * Репозиторий для работы с профилем пользователя.
 * Предоставляет методы для получения и обновления персональных данных.
 */
interface UserProfileRepository {

    /**
     * Получает текущий профиль пользователя
     */
    suspend fun getProfile(): UserProfile

    /**
     * Обновляет имя пользователя
     */
    suspend fun updateName(name: String?)

    /**
     * Обновляет профессию пользователя
     */
    suspend fun updateProfession(profession: String?)

    /**
     * Обновляет уровень опыта пользователя
     */
    suspend fun updateExperienceLevel(experienceLevel: String?)

    /**
     * Включает/выключает персонализацию
     */
    suspend fun updateEnabled(enabled: Boolean)

    /**
     * Обновляет весь профиль целиком
     */
    suspend fun updateProfile(profile: UserProfile)
}
