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

    implementation(libs.kotlinx.coroutinesCore)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.clientCore)
    implementation(libs.ktor.clientCio)
    implementation(libs.ktor.clientContentNegotiation)
    implementation(libs.ktor.serializationKotlinxJson)
    implementation(libs.ktor.clientLogging)
    implementation(libs.logback)
}

application {
    mainClass.set("ru.izhxx.aichallenge.rag.docindexer.app.MainKt")
}
