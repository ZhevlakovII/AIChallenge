package ru.izhxx.aichallenge.logic

import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler

/**
 * Обработчики зависимостей для удобного добавления зависимостей в модули.
 */

/**
 * Добавляет зависимости в commonMain sourceSet.
 * @param dependencies лямбда с объявлением зависимостей
 */
fun KotlinMultiplatformExtension.commonDependencies(dependencies: KotlinDependencyHandler.() -> Unit) {
    sourceSets.commonMain.dependencies(dependencies)
}

/**
 * Добавляет зависимости в androidMain sourceSet.
 * @param dependencies лямбда с объявлением зависимостей
 */
fun KotlinMultiplatformExtension.androidDependencies(dependencies: KotlinDependencyHandler.() -> Unit) {
    sourceSets.androidMain.dependencies(dependencies)
}

/**
 * Добавляет зависимости в iosMain sourceSet.
 * @param dependencies лямбда с объявлением зависимостей
 */
fun KotlinMultiplatformExtension.iosDependencies(dependencies: KotlinDependencyHandler.() -> Unit) {
    sourceSets.iosMain.dependencies(dependencies)
}

/**
 * Добавляет зависимости в jvmMain sourceSet.
 * @param dependencies лямбда с объявлением зависимостей
 */
fun KotlinMultiplatformExtension.jvmDependencies(dependencies: KotlinDependencyHandler.() -> Unit) {
    sourceSets.jvmMain.dependencies(dependencies)
}

/**
 * Добавляет зависимости в commonTest sourceSet.
 * @param dependencies лямбда с объявлением зависимостей
 */
fun KotlinMultiplatformExtension.commonTestDependencies(dependencies: KotlinDependencyHandler.() -> Unit) {
    sourceSets.commonTest.dependencies(dependencies)
}

/**
 * Добавляет зависимость implementation.
 * @param dependencyNotation нотация зависимости (имя библиотеки, координаты и т.д.)
 */
fun DependencyHandler.implementation(dependencyNotation: Any): Dependency? =
    add("implementation", dependencyNotation)

/**
 * Добавляет зависимость api.
 * @param dependencyNotation нотация зависимости (имя библиотеки, координаты и т.д.)
 */
fun DependencyHandler.api(dependencyNotation: Any): Dependency? =
    add("api", dependencyNotation)

/**
 * Добавляет зависимость testImplementation.
 * @param dependencyNotation нотация зависимости (имя библиотеки, координаты и т.д.)
 */
fun DependencyHandler.testImplementation(dependencyNotation: Any): Dependency? =
    add("testImplementation", dependencyNotation)

/**
 * Добавляет зависимость ksp для Android.
 * @param dependencyNotation нотация зависимости (имя библиотеки, координаты и т.д.)
 */
fun DependencyHandler.kspAndroid(dependencyNotation: Any): Dependency? =
    add("kspAndroid", dependencyNotation)

/**
 * Добавляет зависимость ksp для Android.
 * @param dependencyNotation нотация зависимости (имя библиотеки, координаты и т.д.)
 */
fun DependencyHandler.kspJvm(dependencyNotation: Any): Dependency? =
    add("kspJvm", dependencyNotation)


/**
 * Добавляет зависимость debugImplementation.
 * @param dependencyNotation нотация зависимости (имя библиотеки, координаты и т.д.)
 */
fun DependencyHandler.debugImplementation(dependencyNotation: Any): Dependency? =
    add("debugImplementation", dependencyNotation)