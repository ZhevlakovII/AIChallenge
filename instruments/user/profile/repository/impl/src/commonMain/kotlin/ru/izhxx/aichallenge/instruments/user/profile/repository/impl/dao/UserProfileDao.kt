package ru.izhxx.aichallenge.instruments.user.profile.repository.impl.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import ru.izhxx.aichallenge.instruments.user.profile.repository.impl.entity.UserProfileEntity

/**
 * DAO для работы с профилем пользователя в Room Database
 */
@Dao
interface UserProfileDao {

    /**
     * Получает профиль пользователя.
     * Возвращает null если профиль еще не создан.
     */
    @Query("SELECT * FROM user_profile WHERE primaryKey = 0")
    suspend fun getProfile(): UserProfileEntity?

    /**
     * Обновляет имя пользователя
     */
    @Query("UPDATE user_profile SET name = :name WHERE primaryKey = 0")
    suspend fun updateName(name: String?)

    /**
     * Обновляет профессию
     */
    @Query("UPDATE user_profile SET profession = :profession WHERE primaryKey = 0")
    suspend fun updateProfession(profession: String?)

    /**
     * Обновляет уровень опыта
     */
    @Query("UPDATE user_profile SET experienceLevel = :experienceLevel WHERE primaryKey = 0")
    suspend fun updateExperienceLevel(experienceLevel: String?)

    /**
     * Включает/выключает персонализацию
     */
    @Query("UPDATE user_profile SET enabled = :enabled WHERE primaryKey = 0")
    suspend fun updateEnabled(enabled: Boolean)

    /**
     * Вставляет или обновляет профиль целиком
     */
    @Upsert
    suspend fun upsertProfile(profile: UserProfileEntity)
}
