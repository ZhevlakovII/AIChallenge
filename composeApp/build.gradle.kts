
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.hot.reload)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }
    jvm()
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { target ->
        target.binaries.framework {
            baseName = project.name
            isStatic = true
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.koin.android)
            implementation(libs.koin.androidx.compose)
        }
        commonMain.dependencies {
            implementation(projects.features.productassistant.api)
            implementation(projects.features.productassistant.impl)
            implementation(projects.features.pranalyzer.api)
            implementation(projects.features.pranalyzer.impl)
            implementation(projects.core.ui.navigation)

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.androidx.navigation.compose)
            implementation(libs.compose.icons.material)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.kotlinx.datetime)
            implementation(projects.shared.sharedold)
            implementation(projects.rag.docIndexer.core)
            implementation(projects.rag.docIndexer.ollama)

            implementation(projects.instruments.user.profile.model)
            implementation(projects.instruments.user.profile.repository.api)
            implementation(projects.instruments.user.profile.repository.impl)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.koin.core)
            implementation(projects.rag.docIndexer.fsJvm)
            implementation(libs.ktor.client.core)
        }
    }
}

android {
    namespace = "ru.izhxx.aichallenge"
    compileSdk = 36

    defaultConfig {
        applicationId = "ru.izhxx.aichallenge"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            // Отключаем R8 minify для стабильной сборки (проблемы с вырезанием RAG классов)
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "ru.izhxx.aichallenge.MainKt"

        // Увеличиваем heap size для Desktop приложения (для RAG индексации)
        jvmArgs += listOf(
            "-Xmx8G",  // Максимальный размер heap 8GB
            "-Xms1G"    // Начальный размер heap 1GB
        )

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "ru.izhxx.aichallenge"
            packageVersion = "1.0.0"
        }
    }
}

// ===== Анализ зависимостей классов (jdeps) для модуля composeApp =====

val moduleName = "composeApp"

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
        val summary = jdepsSummaryDot.get().asFile
        summary.exists() && findExecutable("dot") != null
    }
    doFirst {
        val outFile = rootProject.layout.projectDirectory.file("docs/architecture/$moduleName-class-deps.svg").asFile
        outFile.parentFile.mkdirs()
        val dotExe = findExecutable("dot")!!
        executable = dotExe
        args(
            "-Tsvg",
            jdepsSummaryDot.get().asFile.absolutePath,
            "-o",
            outFile.absolutePath
        )
    }
}

// ===== Доп. задачи: пакетный уровень, срезы по фичам, раскраска/валидация =====

// Пакетный уровень (verbose:package)
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

// Срез по фиче: -Pfeature=<name> (например, chat)
tasks.register<Exec>("jdepsFeatureGraph") {
    group = "analysis"
    description = "Генерирует DOT-граф зависимостей классов для выбранной фичи (задайте -Pfeature=<name>)"
    notCompatibleWithConfigurationCache("Custom Exec with providers; skip CC for stability")
    dependsOn("jvmJar")
    onlyIf { project.findProperty("feature") != null }
    val feature = (project.findProperty("feature") as String?)
    val featureOut = layout.buildDirectory.dir("reports/jdeps/$moduleName/features/${feature ?: "unknown"}")
    doFirst {
        featureOut.get().asFile.mkdirs()
    }
    val jarFile = tasks.named("jvmJar").flatMap { (it as org.gradle.api.tasks.bundling.Jar).archiveFile }
    val runtimeCp = configurations.getByName("jvmRuntimeClasspath").asPath
    executable = jdepsExecPath.get()
    // Включаем только классы внутри пакета фичи
    args(
        "--multi-release", System.getProperty("java.specification.version"),
        "-verbose:class",
        "-include", "ru\\.izhxx\\.aichallenge\\.features\\.${feature}\\..*",
        "--dot-output", featureOut.get().asFile.absolutePath,
        "-cp", runtimeCp,
        jarFile.get().asFile.absolutePath
    )
}

tasks.register<Sync>("copyFeatureGraphToDocs") {
    group = "analysis"
    description = "Копирует DOT-граф выбранной фичи в docs/architecture/$moduleName/features/<feature>"
    notCompatibleWithConfigurationCache("Uses project layout references; skip CC")
    dependsOn("jdepsFeatureGraph")
    onlyIf { project.findProperty("feature") != null }
    val feature = (project.findProperty("feature") as String?)
    from(layout.buildDirectory.dir("reports/jdeps/$moduleName/features/${feature ?: "unknown"}"))
    into(rootProject.layout.projectDirectory.dir("docs/architecture/$moduleName/features/${feature ?: "unknown"}"))
}

// Раскраска слоёв и подсветка запрещённых зависимостей для class-графа
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
