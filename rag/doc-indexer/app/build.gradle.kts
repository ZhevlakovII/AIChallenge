plugins {
    id("jvm.library")
    application
}

group = "ru.izhxx.aichallenge.rag.docindexer.app"
version = "1.0.0"

dependencies {
    implementation(projects.rag.docIndexer.core)
    implementation(projects.rag.docIndexer.ollama)
    implementation(projects.rag.docIndexer.fsJvm)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.logging)
    implementation(libs.logback.classic)
}

application {
    mainClass.set("ru.izhxx.aichallenge.rag.docindexer.app.MainKt")
}
