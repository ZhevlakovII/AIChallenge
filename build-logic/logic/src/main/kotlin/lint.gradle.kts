import io.gitlab.arturbosch.detekt.Detekt
import ru.izhxx.aichallenge.logic.config.SharedConfig
import ru.izhxx.aichallenge.logic.extensions.libs

/**
 * Плагин для настройки код-стайла (ktlint, detekt) и базовых опций компиляции Kotlin.
 * Вместо отдельных конфигураций в build.gradle.kts, вся логика находится здесь.
 */
plugins {
    id("io.gitlab.arturbosch.detekt")
}

tasks.register<Detekt>("detektFormat") {
    autoCorrect = true
}

tasks.withType<Detekt> {
    // Disable caching
    outputs.upToDateWhen { false }

    reports {
        html.required.set(true)
        xml.required.set(false)
        txt.required.set(false)
    }

    setSource(files(projectDir))
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    baseline.set(file("$rootDir/config/detekt/baseline.xml"))

    include("**/*.kt", "**/*.kts")
    exclude(
        "**/resources/**",
        "**/build/**",
    )
    parallel = true
    buildUponDefaultConfig = true
    allRules = false

    // Target version of the generated JVM bytecode. It is used for type resolution.
    jvmTarget = SharedConfig.JVM_TARGET.target
}

val detektExcludedRoots = setOf("composeApp", "shared", "rag", "instances")
if (detektExcludedRoots.any { project.path == ":$it" || project.path.startsWith(":$it:") }) {
    tasks.withType<Detekt>().configureEach { enabled = false }
}

dependencies {
    detektPlugins(libs.detekt.ruleset.compiler)
    detektPlugins(libs.detekt.formatting)
    detektPlugins(libs.detekt.ruleset.compose)
}
