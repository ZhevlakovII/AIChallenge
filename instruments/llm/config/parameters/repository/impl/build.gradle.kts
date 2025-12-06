
import ru.izhxx.aichallenge.logic.commonDependencies
import ru.izhxx.aichallenge.logic.configurator.config

plugins {
    id("kmp.library")
    id("room")
}

android {
    config("instruments.llm.config.parameters.repository.impl")
}

kotlin {
    commonDependencies {
        // Project
        implementation(projects.instruments.llm.config.parameters.repository.api)

        // Libraries
        implementation(libs.kotlinx.coroutinesCore)
        implementation(libs.koin.core)
    }
}
