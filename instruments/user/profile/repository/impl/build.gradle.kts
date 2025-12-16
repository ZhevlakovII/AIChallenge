import ru.izhxx.aichallenge.logic.commonDependencies
import ru.izhxx.aichallenge.logic.configurator.config

plugins {
    id("shared.library")
    id("room")
}

android {
    config("instruments.user.profile.repository.impl")
}

kotlin {
    commonDependencies {
        // Project dependencies
        implementation(projects.instruments.user.profile.repository.api)
        implementation(projects.instruments.user.profile.model)

        // Libraries
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.koin.core)
    }
}
