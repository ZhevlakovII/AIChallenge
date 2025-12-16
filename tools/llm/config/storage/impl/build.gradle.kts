import ru.izhxx.aichallenge.logic.commonDependencies
import ru.izhxx.aichallenge.logic.configurator.config

plugins {
    id("shared.library")
    id("room")
}

android {
    config("tools.llm.config.storage.impl")
}

kotlin {
    commonDependencies {
        implementation(projects.tools.llm.config.storage.api)
        implementation(projects.tools.llm.config.model)
        implementation(projects.core.url)

        implementation(libs.koin.core)
    }
}