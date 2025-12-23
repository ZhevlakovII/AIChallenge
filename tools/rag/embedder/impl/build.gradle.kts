import ru.izhxx.aichallenge.logic.commonDependencies
import ru.izhxx.aichallenge.logic.configurator.config

plugins {
    id("shared.library")
}

android {
    config("tools.embedder.impl")
}

kotlin {
    commonDependencies {
        implementation(projects.tools.rag.embedder.api)
        implementation(projects.tools.rag.core)
    }
}
