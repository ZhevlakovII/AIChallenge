import ru.izhxx.aichallenge.logic.commonDependencies
import ru.izhxx.aichallenge.logic.configurator.config

plugins {
    id("shared.library")
}

android {
    config("tools.llm.config.model")
}

kotlin {
    commonDependencies {
        implementation(projects.core.url)
    }
}