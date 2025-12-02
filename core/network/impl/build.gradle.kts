import ru.izhxx.aichallenge.logic.commonDependencies
import ru.izhxx.aichallenge.logic.configurator.config

plugins {
    id("kmp.library")
}

android {
    config("core.network.impl")
}

kotlin {
    commonDependencies {
        implementation(projects.core.network.api)
        implementation(projects.core.foundation)

        implementation(libs.koin.core)

        implementation(libs.ktor.clientCore)
        implementation(libs.ktor.clientLogging)
    }
}
