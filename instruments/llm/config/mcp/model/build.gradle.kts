import ru.izhxx.aichallenge.logic.commonDependencies
import ru.izhxx.aichallenge.logic.configurator.config

plugins {
    id("shared.library")
}

android {
    config("instruments.llm.config.mcp.model")
}

kotlin {
    commonDependencies {
        // Libraries
        implementation(libs.kotlinx.serialization.json)
    }
}
