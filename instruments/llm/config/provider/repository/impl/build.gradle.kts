import ru.izhxx.aichallenge.logic.commonDependencies
import ru.izhxx.aichallenge.logic.configurator.config

plugins {
    id("shared.library")
    id("room")
}

android {
    config("instruments.llm.config.provider.repository.api")
}
kotlin {
    commonDependencies {
        // Project
        implementation(projects.instruments.llm.config.provider.repository.api)

        // Libraries
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.koin.core)
    }
}
