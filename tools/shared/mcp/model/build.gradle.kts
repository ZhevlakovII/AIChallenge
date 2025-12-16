import ru.izhxx.aichallenge.logic.commonDependencies
import ru.izhxx.aichallenge.logic.configurator.config

plugins {
    id("shared.library")
}

android {
    config("tools.shared.mcp.model")
}

kotlin {
    commonDependencies {
        implementation(libs.kotlinx.serialization.json)
    }
}
