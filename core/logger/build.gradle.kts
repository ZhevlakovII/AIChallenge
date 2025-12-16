import ru.izhxx.aichallenge.logic.commonDependencies
import ru.izhxx.aichallenge.logic.configurator.config

plugins {
    id("shared.library")
}

android {
    config("core.logger")
}

kotlin {
    commonDependencies {
        implementation(projects.core.buildmode)
        implementation(projects.core.utils)
    }
}
