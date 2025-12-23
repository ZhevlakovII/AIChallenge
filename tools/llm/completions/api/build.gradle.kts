import ru.izhxx.aichallenge.logic.commonDependencies
import ru.izhxx.aichallenge.logic.configurator.config

plugins {
    id("shared.library")
}

android {
    config("tools.llm.completions.api")
}

kotlin {
    commonDependencies {
        implementation(projects.tools.llm.config.model)
        implementation(projects.tools.shared.mcp.model)
        implementation(projects.core.result)
    }
}
