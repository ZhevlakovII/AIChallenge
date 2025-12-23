import ru.izhxx.aichallenge.logic.commonDependencies
import ru.izhxx.aichallenge.logic.configurator.config

plugins {
    id("shared.library")
}

android {
    config("features.chat.navigation")
}

kotlin {
    commonDependencies {
        implementation(projects.core.ui.navigation)
    }
}
