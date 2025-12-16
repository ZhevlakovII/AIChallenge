package ru.izhxx.aichallenge.instruments.user.profile.repository.impl.di

import org.koin.dsl.module
import ru.izhxx.aichallenge.instruments.user.profile.repository.api.UserProfileRepository
import ru.izhxx.aichallenge.instruments.user.profile.repository.impl.UserProfileRepositoryImpl

/**
 * Модуль Koin DI для UserProfileRepository
 * Провайд UserProfileDao осуществляется в корневом (app) модуле через AppDatabase.
 */
val userProfileModule = module {
    factory<UserProfileRepository> {
        UserProfileRepositoryImpl(userProfileDao = get())
    }
}
