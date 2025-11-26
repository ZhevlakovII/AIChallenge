plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.compose.hot.reload) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ktor) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.detekt.plugin) apply false
    id("lint")
}

/* ===== Агрегирующие задачи анализа зависимостей (jdeps) по всем модулям ===== */

tasks.register("generateDependencyGraph") {
    group = "analysis"
    description = "Генерирует DOT-графы зависимостей классов для всех модулей, копирует их в docs/architecture и, при наличии Graphviz, рендерит SVG."
    dependsOn(
        // Генерация DOT
        ":shared:jdepsClassGraph",
        ":composeApp:jdepsClassGraph",
        ":server:jdepsClassGraph",
        // Копирование в docs/
        ":shared:copyClassGraphToDocs",
        ":composeApp:copyClassGraphToDocs",
        ":server:copyClassGraphToDocs",
        // Рендер SVG (graceful no-op если Graphviz не установлен)
        ":shared:renderClassGraphSvg",
        ":composeApp:renderClassGraphSvg",
        ":server:renderClassGraphSvg",
    )
}

/* Пакетный уровень (-verbose:package) */
tasks.register("generatePackageDependencyGraph") {
    group = "analysis"
    description = "Генерирует пакетные DOT-графы (-verbose:package) и копирует их в docs/architecture/**/package."
    dependsOn(
        // Генерация
        ":composeApp:jdepsPackageGraph",
        ":shared:jdepsPackageGraph",
        ":server:jdepsPackageGraph",
        // Копирование
        ":composeApp:copyPackageGraphToDocs",
        ":shared:copyPackageGraphToDocs",
        ":server:copyPackageGraphToDocs",
    )
}

/* Раскраска/валидация и рендер декорированных графов */
tasks.register("decorateDependencyGraph") {
    group = "analysis"
    description = "Раскрашивает узлы (presentation/domain/data), подсвечивает нарушения и рендерит декорированные SVG для всех модулей."
    dependsOn(
        ":composeApp:decorateClassGraph",
        ":shared:decorateClassGraph",
        ":server:decorateClassGraph",
        ":composeApp:renderDecoratedClassGraphSvg",
        ":shared:renderDecoratedClassGraphSvg",
        ":server:renderDecoratedClassGraphSvg",
    )
}

/* Срез по фиче (-Pfeature=<name>), применимо к composeApp */
tasks.register("generateFeatureGraph") {
    group = "analysis"
    description = "Генерирует DOT-граф для выбранной фичи из composeApp. Используйте -Pfeature=<name> (например, chat). Результат: docs/architecture/composeApp/features/<name>/summary.dot"
    dependsOn(
        ":composeApp:jdepsFeatureGraph",
        ":composeApp:copyFeatureGraphToDocs",
    )
}
