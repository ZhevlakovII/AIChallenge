package ru.izhxx.aichallenge.instruments.user.profile.repository.impl.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity для хранения профиля пользователя в базе данных.
 * Используется singleton pattern (primaryKey = 0L) - только одна запись.
 */
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey
    val primaryKey: Long = 0L,
    val name: String?,
    val profession: String?,
    val experienceLevel: String?,
    val enabled: Boolean
)
