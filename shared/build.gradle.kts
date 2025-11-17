import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    jvm()

    sourceSets {
        jvmMain.dependencies {
            implementation(libs.ktor.clientJava)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.androidx.datastore.preferences)
        }

        androidMain.dependencies {
            implementation(libs.ktor.clientAndroid)
            implementation(libs.androidx.datastore.preferences)
        }

        commonMain.dependencies {
            // Ktor Client
            implementation(libs.ktor.clientCore)
            implementation(libs.ktor.clientContentNegotiation)
            implementation(libs.ktor.clientLogging)
            implementation(libs.ktor.serializationKotlinxJson)
            implementation(libs.ktor.clientWebsockets)

            // Kotlinx Serialization
            implementation(libs.kotlinx.serialization.json)

            // Coroutines
            implementation(libs.kotlinx.coroutinesCore)
            
            // Koin
            implementation(libs.koin.core)

            // DataStore
            implementation(libs.androidx.datastore.preferences)

            // Room
            implementation(libs.room.runtime)
            implementation(libs.sqlite.bundled)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "ru.izhxx.aichallenge.shared"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
}

dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspJvm", libs.room.compiler)
}

room {
    schemaDirectory("$projectDir/schemas")
}
