import ru.izhxx.aichallenge.logic.commonDependencies
import ru.izhxx.aichallenge.logic.configurator.config

plugins {
    id("shared.library")
}

android {
    config("tools.embedder.api")
}

kotlin {
    commonDependencies {
        implementation(projects.tools.rag.core)
    }
}
