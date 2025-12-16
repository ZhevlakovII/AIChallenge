import ru.izhxx.aichallenge.logic.commonDependencies
import ru.izhxx.aichallenge.logic.configurator.config
import ru.izhxx.aichallenge.logic.jvmDependencies

plugins {
    id("shared.library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    // Namespace будет ru.izhxx.aichallenge.rag.docindexer.ollama
    config("rag.docindexer.ollama")
}

kotlin {
    commonDependencies {
        implementation(project(":rag:doc-indexer:core"))
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.kotlinx.serialization.json)
        implementation(libs.ktor.client.core)
        implementation(libs.ktor.client.content.negotiation)
        implementation(libs.ktor.serialization.kotlinx.json)
    }
    jvmDependencies {
        implementation(libs.ktor.client.cio)
        implementation(libs.ktor.client.logging)
    }
}
