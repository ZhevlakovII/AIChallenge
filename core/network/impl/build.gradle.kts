import ru.izhxx.aichallenge.logic.commonDependencies
import ru.izhxx.aichallenge.logic.configurator.config

plugins {
    id("shared.library")
}

android {
    config("core.network.impl")
}

kotlin {
    commonDependencies {
        implementation(projects.core.network.api)
        implementation(projects.core.buildmode)
        implementation(projects.core.logger)

        implementation(libs.koin.core)

        implementation(libs.ktor.client.core)
        implementation(libs.ktor.client.logging)

        implementation(libs.kotlinx.serialization.json)
    }
}
