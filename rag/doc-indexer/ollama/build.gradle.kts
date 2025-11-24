import ru.izhxx.aichallenge.logic.commonDependencies
import ru.izhxx.aichallenge.logic.configurator.config
import ru.izhxx.aichallenge.logic.jvmDependencies

plugins {
    id("frontend.library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    // Namespace будет ru.izhxx.aichallenge.rag.docindexer.ollama
    config("rag.docindexer.ollama")
}

kotlin {
    commonDependencies {
        implementation(project(":rag:doc-indexer:core"))
        implementation(libs.kotlinx.coroutinesCore)
        implementation(libs.kotlinx.serialization.json)
        implementation(libs.ktor.clientCore)
        implementation(libs.ktor.clientContentNegotiation)
        implementation(libs.ktor.serializationKotlinxJson)
    }
    jvmDependencies {
        implementation(libs.ktor.clientCio)
        implementation(libs.ktor.clientLogging)
    }
}
