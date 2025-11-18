import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.File

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
            implementation(libs.ktor.clientCio)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.androidx.datastore.preferences)
        }

        androidMain.dependencies {
            implementation(libs.ktor.clientOkhttp)
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

// ===== Анализ зависимостей классов (jdeps) для модуля shared =====

val moduleName = "shared"

fun findExecutable(name: String): String? {
    val os = System.getProperty("os.name").lowercase()
    val ext = if (os.contains("win")) ".exe" else ""
    val path = System.getenv("PATH") ?: return null
    return path.split(File.pathSeparator)
        .map { File(it, "$name$ext") }
        .firstOrNull { it.exists() && it.canExecute() }
        ?.absolutePath
}

val jdepsOutputDir = layout.buildDirectory.dir("reports/jdeps/$moduleName")
val jdepsSummaryDot = jdepsOutputDir.map { it.file("summary.dot") }

val jdepsExecPath = providers.provider {
    val os = System.getProperty("os.name").lowercase()
    val ext = if (os.contains("win")) ".exe" else ""
    val javaHome = System.getProperty("java.home")
    val candidate = File(javaHome, "bin/jdeps$ext")
    candidate.absolutePath
}

tasks.register<Exec>("jdepsClassGraph") {
    group = "analysis"
    description = "Генерирует DOT-граф зависимостей классов для $moduleName через jdeps"
    notCompatibleWithConfigurationCache("Custom Exec with providers; skip CC for stability")
    dependsOn("jvmJar")
    doFirst {
        jdepsOutputDir.get().asFile.mkdirs()
    }
    val jarFile = tasks.named("jvmJar").flatMap { (it as org.gradle.api.tasks.bundling.Jar).archiveFile }
    val runtimeCp = configurations.getByName("jvmRuntimeClasspath").asPath
    executable = jdepsExecPath.get()
    args(
        "--multi-release", System.getProperty("java.specification.version"),
        "-verbose:class",
        "-include", "ru\\.izhxx\\..*",
        "--dot-output", jdepsOutputDir.get().asFile.absolutePath,
        "-cp", runtimeCp,
        jarFile.get().asFile.absolutePath
    )
}

tasks.register<Sync>("copyClassGraphToDocs") {
    group = "analysis"
    description = "Копирует DOT-граф(ы) в docs/architecture/$moduleName"
    notCompatibleWithConfigurationCache("Uses project layout references; skip CC")
    dependsOn("jdepsClassGraph")
    from(jdepsOutputDir)
    into(rootProject.layout.projectDirectory.dir("docs/architecture/$moduleName"))
}

tasks.register<Exec>("renderClassGraphSvg") {
    group = "analysis"
    description = "Рендерит summary.dot в SVG (если установлен Graphviz)"
    notCompatibleWithConfigurationCache("Exec with external tool; skip CC")
    dependsOn("jdepsClassGraph")
    onlyIf {
        jdepsSummaryDot.get().asFile.exists() && findExecutable("dot") != null
    }
    doFirst {
        val outFile = rootProject.layout.projectDirectory.file("docs/architecture/$moduleName-class-deps.svg").asFile
        outFile.parentFile.mkdirs()
        val dotExe = findExecutable("dot") ?: return@doFirst
        commandLine(
            dotExe,
            "-Tsvg",
            jdepsSummaryDot.get().asFile.absolutePath,
            "-o",
            outFile.absolutePath
        )
    }
}

// ===== Доп. задачи: пакетный уровень и раскраска/валидация =====

val jdepsPkgOutputDir = layout.buildDirectory.dir("reports/jdeps/$moduleName/package")
val jdepsPkgSummaryDot = jdepsPkgOutputDir.map { it.file("summary.dot") }

tasks.register<Exec>("jdepsPackageGraph") {
    group = "analysis"
    description = "Генерирует DOT-граф зависимостей на уровне ПАКЕТОВ для $moduleName через jdeps"
    notCompatibleWithConfigurationCache("Custom Exec with providers; skip CC for stability")
    dependsOn("jvmJar")
    doFirst {
        jdepsPkgOutputDir.get().asFile.mkdirs()
    }
    val jarFile = tasks.named("jvmJar").flatMap { (it as org.gradle.api.tasks.bundling.Jar).archiveFile }
    val runtimeCp = configurations.getByName("jvmRuntimeClasspath").asPath
    executable = jdepsExecPath.get()
    args(
        "--multi-release", System.getProperty("java.specification.version"),
        "-verbose:package",
        "-include", "ru\\.izhxx\\..*",
        "--dot-output", jdepsPkgOutputDir.get().asFile.absolutePath,
        "-cp", runtimeCp,
        jarFile.get().asFile.absolutePath
    )
}

tasks.register<Sync>("copyPackageGraphToDocs") {
    group = "analysis"
    description = "Копирует пакетный DOT-граф(ы) в docs/architecture/$moduleName/package"
    notCompatibleWithConfigurationCache("Uses project layout references; skip CC")
    dependsOn("jdepsPackageGraph")
    from(jdepsPkgOutputDir)
    into(rootProject.layout.projectDirectory.dir("docs/architecture/$moduleName/package"))
}

tasks.register("decorateClassGraph") {
    group = "analysis"
    description = "Раскрашивает узлы по слоям (presentation/domain/data) и подсвечивает запрещённые зависимости (красным)"
    notCompatibleWithConfigurationCache("String processing of DOT; skip CC")
    dependsOn("copyClassGraphToDocs")
    doLast {
        val input = rootProject.layout.projectDirectory.file("docs/architecture/$moduleName/summary.dot").asFile
        if (!input.exists()) return@doLast

        val text = input.readText()
        val lines = text.lines()

        val presentation = Regex("""^ru\.izhxx\.aichallenge\.features\..*""")
        val domain = Regex("""^ru\.izhxx\.aichallenge\.domain(\..*)?""")
        val data = Regex("""^ru\.izhxx\.aichallenge\.data(\..*)?""")

        val edgePattern = Regex("""\s*\"([^\"]+)\"\s*->\s*\"([^\"]+)\"(\s*\[[^\]]*])?\s*;?\s*$""")
        val nodePattern = Regex("""^\s*\"([^\"]+)\"(?:\s*\[.*])?\s*;?\s*$""")

        val nodes = mutableSetOf<String>()
        lines.forEach { line ->
            val m = edgePattern.matchEntire(line)
            if (m != null) {
                nodes += m.groupValues[1]
                nodes += m.groupValues[2]
            } else {
                val n = nodePattern.matchEntire(line)?.groupValues?.getOrNull(1)
                if (n != null) nodes += n
            }
        }

        fun layerOf(n: String): String? = when {
            presentation.containsMatchIn(n) -> "presentation"
            domain.containsMatchIn(n) -> "domain"
            data.containsMatchIn(n) -> "data"
            else -> null
        }

        val decorated = StringBuilder()
        val violations = mutableListOf<String>()
        var injected = false

        lines.forEach { line ->
            if (!injected && line.contains("{")) {
                decorated.append(line).append('\n')
                // Вставляем определения узлов с цветами
                nodes.forEach { n ->
                    when (layerOf(n)) {
                        "presentation" -> decorated.append("  \"").append(n).append("\" [style=filled, fillcolor=\"#80b1d3\"];").append('\n')
                        "domain" -> decorated.append("  \"").append(n).append("\" [style=filled, fillcolor=\"#8dd3c7\"];").append('\n')
                        "data" -> decorated.append("  \"").append(n).append("\" [style=filled, fillcolor=\"#fdb462\"];").append('\n')
                    }
                }
                injected = true
            } else {
                val m = edgePattern.matchEntire(line)
                if (m != null) {
                    val from = m.groupValues[1]
                    val to = m.groupValues[2]
                    val isDto = to.contains(".data.model.") && to.endsWith("DTO")
                    val forbidden =
                        // Presentation -> Data (UI не должна зависеть от Data-реализаций/DTO)
                        (presentation.containsMatchIn(from) && data.containsMatchIn(to)) ||
                        // Domain -> Data (Domain не должен зависеть от Data)
                        (domain.containsMatchIn(from) && data.containsMatchIn(to)) ||
                        // Domain -> Presentation (Domain не должен зависеть от UI)
                        (domain.containsMatchIn(from) && presentation.containsMatchIn(to)) ||
                        // Data -> Presentation (Data не должен зависеть от UI)
                        (data.containsMatchIn(from) && presentation.containsMatchIn(to)) ||
                        // DTO в Presentation/Domain запрещены
                        ((presentation.containsMatchIn(from) || domain.containsMatchIn(from)) && isDto)
                    if (forbidden) {
                        violations += "\"$from\" -> \"$to\""
                        decorated.append("  \"").append(from).append("\" -> \"").append(to).append("\" [color=red, penwidth=2, label=\"FORBIDDEN\"];").append('\n')
                    } else {
                        decorated.append(line).append('\n')
                    }
                } else {
                    decorated.append(line).append('\n')
                }
            }
        }

        val outDot = rootProject.layout.projectDirectory.file("docs/architecture/$moduleName/summary.decorated.dot").asFile
        outDot.writeText(decorated.toString())

        val outViolations = rootProject.layout.projectDirectory.file("docs/architecture/$moduleName/violations.txt").asFile
        outViolations.writeText(
            if (violations.isEmpty()) "Нарушений не найдено"
            else violations.joinToString(separator = "\n")
        )
    }
}

tasks.register<Exec>("renderDecoratedClassGraphSvg") {
    group = "analysis"
    description = "Рендерит summary.decorated.dot в SVG (если установлен Graphviz)"
    notCompatibleWithConfigurationCache("Exec with external tool; skip CC")
    dependsOn("decorateClassGraph")
    onlyIf {
        val inFile = rootProject.layout.projectDirectory.file("docs/architecture/$moduleName/summary.decorated.dot").asFile
        inFile.exists() && (System.getenv("PATH")?.split(File.pathSeparator)?.any { File(it, "dot").exists() || File(it, "dot.exe").exists() } == true)
    }
    doFirst {
        val inFile = rootProject.layout.projectDirectory.file("docs/architecture/$moduleName/summary.decorated.dot").asFile
        val outFile = rootProject.layout.projectDirectory.file("docs/architecture/$moduleName/summary.decorated.svg").asFile
        outFile.parentFile.mkdirs()
        // Поиск dot
        val path = System.getenv("PATH") ?: ""
        val exe = path.split(File.pathSeparator)
            .map { File(it, if (System.getProperty("os.name").lowercase().contains("win")) "dot.exe" else "dot") }
            .firstOrNull { it.exists() && it.canExecute() }
            ?: return@doFirst
        commandLine(
            exe.absolutePath,
            "-Tsvg",
            inFile.absolutePath,
            "-o",
            outFile.absolutePath
        )
    }
}
