import ru.izhxx.aichallenge.logic.commonDependencies
import ru.izhxx.aichallenge.logic.configurator.config

plugins {
    id("shared.library")
}

android {
    config("instruments.llm.config.provider.repository.api")
}

kotlin {
    commonDependencies {
        // Project
        api(projects.instruments.llm.config.provider.model)

        // Libraries
        implementation(libs.kotlinx.coroutines.core)
    }
}
