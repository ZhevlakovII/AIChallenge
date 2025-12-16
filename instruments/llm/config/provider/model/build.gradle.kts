import ru.izhxx.aichallenge.logic.commonDependencies
import ru.izhxx.aichallenge.logic.configurator.config

plugins {
    id("shared.library")
}

android {
    config("instruments.llm.config.provider.model")
}

kotlin {
    commonDependencies {
        api(projects.core.url)
    }
}
