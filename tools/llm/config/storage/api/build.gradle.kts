import ru.izhxx.aichallenge.logic.commonDependencies
import ru.izhxx.aichallenge.logic.configurator.config

plugins {
    id("shared.library")
}

android {
    config("tools.llm.config.storage.api")
}

kotlin {
    commonDependencies {
        implementation(projects.tools.llm.config.model)
        implementation(projects.core.url)
    }
}