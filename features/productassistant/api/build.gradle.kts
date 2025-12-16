import ru.izhxx.aichallenge.logic.commonDependencies
import ru.izhxx.aichallenge.logic.configurator.config

plugins {
    id("shared.library")
}

android {
    config("ru.izhxx.aichallenge.features.productassistant.api")
}

kotlin {
    commonDependencies {
        implementation(projects.core.ui.navigation)
    }
}
