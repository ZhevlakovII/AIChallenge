import ru.izhxx.aichallenge.logic.androidDependencies
import ru.izhxx.aichallenge.logic.commonDependencies
import ru.izhxx.aichallenge.logic.configurator.config
import ru.izhxx.aichallenge.logic.iosDependencies
import ru.izhxx.aichallenge.logic.jvmDependencies

plugins {
    id("shared.library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    config("instruments.llm.interactions.impl")
}

kotlin {
    commonDependencies {
        // Project
        implementation(projects.instruments.llm.interaction.api)
        implementation(projects.core.foundation)
        implementation(projects.core.network.api)
        implementation(projects.instruments.llm.config.mcp.model)
        implementation(projects.instruments.llm.config.parameters.model)
        implementation(projects.instruments.llm.config.provider.model)

        // Libraries
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.ktor.client.core)
        implementation(libs.ktor.client.content.negotiation)
        implementation(libs.ktor.serialization.kotlinx.json)
        implementation(libs.koin.core)
        implementation(libs.kotlinx.serialization.json)
    }

    androidDependencies {
        implementation(libs.ktor.client.okhttp)
    }
    jvmDependencies {
        implementation(libs.ktor.client.cio)
    }
    iosDependencies {
        implementation(libs.ktor.client.darwin)
    }
}
