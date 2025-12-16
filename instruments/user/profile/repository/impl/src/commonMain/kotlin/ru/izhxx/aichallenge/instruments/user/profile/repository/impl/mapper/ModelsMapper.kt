package ru.izhxx.aichallenge.instruments.user.profile.repository.impl.mapper

import ru.izhxx.aichallenge.instruments.user.profile.model.UserProfile
import ru.izhxx.aichallenge.instruments.user.profile.repository.impl.entity.UserProfileEntity

/**
 * Преобразует Entity в Domain модель
 */
internal fun UserProfileEntity.toDomain(): UserProfile {
    return UserProfile(
        name = name,
        profession = profession,
        experienceLevel = experienceLevel,
        enabled = enabled
    )
}

/**
 * Преобразует Domain модель в Entity
 */
internal fun UserProfile.toEntity(): UserProfileEntity {
    return UserProfileEntity(
        primaryKey = 0L, // Singleton
        name = name,
        profession = profession,
        experienceLevel = experienceLevel,
        enabled = enabled
    )
}
