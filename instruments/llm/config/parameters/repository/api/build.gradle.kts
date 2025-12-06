import ru.izhxx.aichallenge.logic.commonDependencies
import ru.izhxx.aichallenge.logic.configurator.config

plugins {
    id("kmp.library")
}

android {
    config("instruments.llm.config.parameters.repository.api")
}

kotlin {
    commonDependencies {
        // Project
        api(projects.instruments.llm.config.parameters.model)

        // Libraries
        implementation(libs.kotlinx.coroutinesCore)
    }
}
