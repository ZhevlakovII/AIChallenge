plugins {
    id("backend.library")
    application
}

dependencies {
    implementation(project(":rag:doc-indexer:core"))
    implementation(project(":rag:doc-indexer:ollama"))
    implementation(project(":rag:doc-indexer:fs-jvm"))

    implementation(libs.kotlinx.coroutinesCore)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.clientCio)
    implementation(libs.ktor.clientContentNegotiation)
    implementation(libs.ktor.serializationKotlinxJson)
    implementation(libs.logback)
}

java {
    toolchain {
        languageVersion.set(org.gradle.jvm.toolchain.JavaLanguageVersion.of(17))
    }
}

application {
    mainClass.set("ru.izhxx.aichallenge.rag.docindexer.app.MainKt")
}
