import ru.izhxx.aichallenge.logic.commonDependencies
import ru.izhxx.aichallenge.logic.configurator.config

plugins {
    id("kmp.library")
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
        implementation(libs.kotlinx.coroutinesCore)
        implementation(libs.ktor.clientCore)
        implementation(libs.koin.core)
        implementation(libs.kotlinx.serialization.json)
    }
}
