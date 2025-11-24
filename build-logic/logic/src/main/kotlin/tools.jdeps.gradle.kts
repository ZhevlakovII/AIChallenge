/*
 * Convention plugin: tools.jdeps
 * Универсальные задачи анализа зависимостей с использованием jdeps и Graphviz (dot).
 * Работает и для KMP (берет jvmJar/jvmRuntimeClasspath) и для JVM-проектов (jar/runtimeClasspath).
 */
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.bundling.Jar
import java.io.File

// Общие параметры по умолчанию
val moduleName = project.name
val includeRegex = "ru\\\\.izhxx\\\\..*"
val featureRootRegex = "ru\\\\.izhxx\\\\.aichallenge\\\\.features"

// Определяем jar-задачу и runtime classpath в зависимости от типа проекта (KMP vs JVM)
val hasJvmJar = tasks.names.contains("jvmJar")
val jarTaskName = if (hasJvmJar) "jvmJar" else "jar"
val runtimeConfName = if (configurations.names.contains("jvmRuntimeClasspath")) "jvmRuntimeClasspath" else "runtimeClasspath"

// Нахождение исполнимых файлов в PATH
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

val jdepsPkgOutputDir = layout.buildDirectory.dir("reports/jdeps/$moduleName/package")
val jdepsPkgSummaryDot = jdepsPkgOutputDir.map { it.file("summary.dot") }

val jdepsExecPath = providers.provider {
    val os = System.getProperty("os.name").lowercase()
    val ext = if (os.contains("win")) ".exe" else ""
    val javaHome = System.getProperty("java.home")
    File(javaHome, "bin/jdeps$ext").absolutePath
}

// ---- Класс-уровень графа (verbose:class) ----
tasks.register<Exec>("jdepsClassGraph") {
    group = "analysis"
    description = "Генерирует DOT-граф зависимостей классов для $moduleName через jdeps"
    notCompatibleWithConfigurationCache("Custom Exec with providers; skip CC for stability")
    dependsOn(jarTaskName)
    doFirst {
        jdepsOutputDir.get().asFile.mkdirs()
    }
    val jarFile = tasks.named(jarTaskName).flatMap { (it as Jar).archiveFile }
    val runtimeCp = configurations.getByName(runtimeConfName).asPath
    executable = jdepsExecPath.get()
    args(
        "--multi-release", System.getProperty("java.specification.version"),
        "-verbose:class",
        "-include", includeRegex,
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

// ---- Пакет-уровень графа (verbose:package) ----
tasks.register<Exec>("jdepsPackageGraph") {
    group = "analysis"
    description = "Генерирует DOT-граф зависимостей на уровне ПАКЕТОВ для $moduleName через jdeps"
    notCompatibleWithConfigurationCache("Custom Exec with providers; skip CC for stability")
    dependsOn(jarTaskName)
    doFirst {
        jdepsPkgOutputDir.get().asFile.mkdirs()
    }
    val jarFile = tasks.named(jarTaskName).flatMap { (it as Jar).archiveFile }
    val runtimeCp = configurations.getByName(runtimeConfName).asPath
    executable = jdepsExecPath.get()
    args(
        "--multi-release", System.getProperty("java.specification.version"),
        "-verbose:package",
        "-include", includeRegex,
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

// ---- Срез по фиче: -Pfeature=<name> ----
tasks.register<Exec>("jdepsFeatureGraph") {
    group = "analysis"
    description = "Генерирует DOT-граф зависимостей классов для выбранной фичи (задайте -Pfeature=<name>)"
    notCompatibleWithConfigurationCache("Custom Exec with providers; skip CC for stability")
    dependsOn(jarTaskName)
    onlyIf { project.findProperty("feature") != null }
    val feature = (project.findProperty("feature") as String?)
    val featureOut = layout.buildDirectory.dir("reports/jdeps/$moduleName/features/${feature ?: "unknown"}")
    doFirst {
        featureOut.get().asFile.mkdirs()
    }
    val jarFile = tasks.named(jarTaskName).flatMap { (it as Jar).archiveFile }
    val runtimeCp = configurations.getByName(runtimeConfName).asPath
    executable = jdepsExecPath.get()
    args(
        "--multi-release", System.getProperty("java.specification.version"),
        "-verbose:class",
        "-include", "$featureRootRegex\\.${feature}\\..*",
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

// ---- Раскраска class-графа и подсветка запрещенных зависимостей ----
tasks.register("decorateClassGraph") {
    group = "analysis"
    description = "Раскрашивает узлы по слоям (presentation/domain/data) и подсвечивает запрещенные зависимости (красным)"
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
                        (presentation.containsMatchIn(from) && data.containsMatchIn(to)) ||
                        (domain.containsMatchIn(from) && data.containsMatchIn(to)) ||
                        (domain.containsMatchIn(from) && presentation.containsMatchIn(to)) ||
                        (data.containsMatchIn(from) && presentation.containsMatchIn(to)) ||
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
        inFile.exists() && findExecutable("dot") != null
    }
    doFirst {
        val inFile = rootProject.layout.projectDirectory.file("docs/architecture/$moduleName/summary.decorated.dot").asFile
        val outFile = rootProject.layout.projectDirectory.file("docs/architecture/$moduleName/summary.decorated.svg").asFile
        outFile.parentFile.mkdirs()
        val dotExe = findExecutable("dot") ?: return@doFirst
        commandLine(
            dotExe,
            "-Tsvg",
            inFile.absolutePath,
            "-o",
            outFile.absolutePath
        )
    }
}
