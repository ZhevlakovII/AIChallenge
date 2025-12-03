package ru.izhxx.aichallenge.logic.configurator

import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import ru.izhxx.aichallenge.logic.config.SharedConfig

/**
 * Настройка общих параметров для KMP модуля.
 */
fun KotlinMultiplatformExtension.config() {
    applyDefaultHierarchyTemplate()
    jvmToolchain(SharedConfig.JVM_VERSION)
    androidTarget {
        compilerOptions {
            jvmTarget.set(SharedConfig.JVM_TARGET)
        }
    }
    jvm()
//    listOf(
//        iosX64(),
//        iosArm64(),
//        iosSimulatorArm64()
//    ).forEach { target ->
//        target.binaries.framework {
//            baseName = project.name
//            isStatic = true
//        }
//    }
}