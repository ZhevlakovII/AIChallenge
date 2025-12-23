import ru.izhxx.aichallenge.logic.commonDependencies
import ru.izhxx.aichallenge.logic.configurator.config

plugins {
    id("shared.library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    config("tools.llm.completions.impl")
}

kotlin {
    commonDependencies {
        implementation(projects.tools.llm.completions.api)
        implementation(projects.tools.shared.mcp.model)
        implementation(projects.core.errors)
        implementation(projects.core.network.api)
        implementation(projects.core.result)
        implementation(projects.core.safecall)
        implementation(projects.core.url)

        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.kotlinx.serialization.json)
        implementation(libs.koin.core)
        implementation(libs.ktor.client.core)
        implementation(libs.ktor.client.content.negotiation)
        implementation(libs.ktor.serialization.kotlinx.json)
    }
}
