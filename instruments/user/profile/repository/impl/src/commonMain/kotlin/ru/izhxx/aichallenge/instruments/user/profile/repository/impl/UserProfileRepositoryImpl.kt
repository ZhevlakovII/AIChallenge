package ru.izhxx.aichallenge.instruments.user.profile.repository.impl

import ru.izhxx.aichallenge.instruments.user.profile.model.UserProfile
import ru.izhxx.aichallenge.instruments.user.profile.repository.api.UserProfileRepository
import ru.izhxx.aichallenge.instruments.user.profile.repository.impl.dao.UserProfileDao
import ru.izhxx.aichallenge.instruments.user.profile.repository.impl.mapper.toDomain
import ru.izhxx.aichallenge.instruments.user.profile.repository.impl.mapper.toEntity

/**
 * Реализация репозитория профиля пользователя через Room Database
 */
internal class UserProfileRepositoryImpl(
    private val userProfileDao: UserProfileDao
) : UserProfileRepository {

    override suspend fun getProfile(): UserProfile {
        // Возвращаем пустой профиль если запись еще не создана
        val entity = userProfileDao.getProfile()
        return entity?.toDomain() ?: UserProfile()
    }

    override suspend fun updateName(name: String?) {
        ensureProfileExists()
        userProfileDao.updateName(name)
    }

    override suspend fun updateProfession(profession: String?) {
        ensureProfileExists()
        userProfileDao.updateProfession(profession)
    }

    override suspend fun updateExperienceLevel(experienceLevel: String?) {
        ensureProfileExists()
        userProfileDao.updateExperienceLevel(experienceLevel)
    }

    override suspend fun updateEnabled(enabled: Boolean) {
        ensureProfileExists()
        userProfileDao.updateEnabled(enabled)
    }

    override suspend fun updateProfile(profile: UserProfile) {
        userProfileDao.upsertProfile(profile.toEntity())
    }

    /**
     * Проверяет существование профиля и создает пустой если нужно
     */
    private suspend fun ensureProfileExists() {
        if (userProfileDao.getProfile() == null) {
            userProfileDao.upsertProfile(UserProfile().toEntity())
        }
    }
}
