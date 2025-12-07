import ru.izhxx.aichallenge.logic.commonDependencies
import ru.izhxx.aichallenge.logic.configurator.config

plugins {
    id("kmp.library")
}

android {
    config("instruments.llm.interactions.api")
}

kotlin {
    commonDependencies {
        // Project
        implementation(projects.core.foundation)
        implementation(projects.instruments.llm.config.mcp.model)
        implementation(projects.instruments.llm.config.parameters.model)
        implementation(projects.instruments.llm.config.provider.model)

        // Libraries
        implementation(libs.kotlinx.coroutinesCore)
    }
}
