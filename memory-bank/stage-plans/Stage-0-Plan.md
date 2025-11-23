# Stage 0 — Подготовка инфраструктуры и выравнивание toolchain

Документ: Stage-0-Plan  
Основание: ADR-2025-11-21 (архитектура мультимодульного KMP), Migration Plan (memory-bank/migration-plan.md)  
Цель этапа: подготовить инфраструктуру для безопасной миграции на мультимодульную архитектуру, зафиксировать инварианты, выровнять Java/Kotlin/KSP, ввести контроль зависимостей (jdeps/Graphviz), обновить Memory Bank и убедиться, что текущая монолитная структура собирается и верифицируется с новыми правилами.

Инварианты (Stage 0)
- Безопасность для продукта: отсутствуют изменения функциональности (только инфраструктурные правки).
- Единый Java Toolchain 21 для всех модулей.
- Принципы зависимостей заданы и визуализируются, но жесткие запреты (build fail на горизонтальные зависимости) будут включаться позже (Stage 3+).
- Документация (Memory Bank) — Single Source of Truth: отражает актуальные решения и ссылки на артефакты.

Скоуп Stage 0
- Memory Bank: выпуск Stage-0-Plan, ссылки в activeContext, обновление progress.
- Gradle/Toolchain: выравнивание Java 21, проверка Kotlin/KSP матрицы совместимости и план корректировки.
- Конвенции: убедиться в включенных фичах (TYPESAFE_PROJECT_ACCESSORS, Foojay Toolchain Resolver), выравнивание versions catalog.
- KSP/Room: убедиться, что KSP применяется для android/jvm таргетов, настроены схемы Room.
- Контроль зависимостей: прогнать jdeps/Graphviz пайплайн на всех модулях, выгрузить артефакты в docs/.
- Валидация: полная сборка (assemble, test где применимо), отсутствуют регрессы.

Вне скоупа Stage 0
- Фактическое разбиение на модули и перенос кодовой базы.
- Введение жестких build-правил на запрет горизонтальных зависимостей (запланировано после стабилизации пайплайна — Stage 3+).
- Изменения API/контрактов.

Артефакты на входе
- Текущая кодовая база (:composeApp, :server, :shared).
- settings.gradle.kts (TYPESAFE_PROJECT_ACCESSORS, Foojay, includes).
- build.gradle.kts (root) с alias-плагинами, jdeps-пайплайном.
- gradle/libs.versions.toml (версии kotlin, agp, compose, ktor, ksp, room).
- Модули с jdeps/Graphviz задачами.
- Memory Bank (migration-plan.md, glossary.md, sessions, progress, activeContext).

Артефакты на выходе
- Обновлённый Memory Bank:
  - memory-bank/stage-plans/Stage-0-Plan.md (этот документ).
  - memory-bank/activeContext.md — ссылки на Migration Plan и Stage-0-Plan.
  - memory-bank/progress.md — закрыты задачи “update memory bank → готовность к Stage 0”.
- Выравненный toolchain:
  - Java 21 для всех Android/JVM частей.
  - Решение по матрице Kotlin/KSP (см. ниже).
- Отчёты jdeps/Graphviz в docs/ (class graph, package graph, decorated graph).
- Зафиксированные решения в changelog/PR-заметках (описание затронутых конфигов).

Риски и допущения
- KSP/Room на KMP: различия плагинных версий могут вызвать ошибки генерации. Требуется явная синхронизация версий.
- Compose Multiplatform + Kotlin/AGP: возможны несовместимости с конкретными minor-версиями.
- JDK/JRE пути: доверяем Foojay Toolchain Resolver и Kotlin jvmToolchain(21); локальные dev-машины должны иметь доступ к соответствующим toolchain.

План работ (пошагово)

1) Memory Bank — актуализация (этот репозиторий)
- Создать Stage-0-Plan (memory-bank/stage-plans/Stage-0-Plan.md) — DONE в этом PR.
- Обновить memory-bank/activeContext.md:
  - Добавить ссылки на Migration Plan и Stage-0-Plan.
  - В разделе “Current Focus” указать Stage 0.
- Обновить memory-bank/progress.md:
  - Закрыть “update memory bank” (этап подготовки) и отметить “готов к Stage 0”.
  - Добавить чекпоинт “Stage 0 — выполнено” после технических правок и проверок.
- При необходимости обновить memory-bank/sessions/ с записью о принятом плане.

2) Gradle/Toolchain — выравнивание Java 21
- Проверить settings.gradle.kts:
  - enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS").
  - Убедиться, что подключен Foojay Toolchain Resolver (через pluginManagement или отдельную конфигурацию).
- Root build.gradle.kts:
  - Централизованно настраивать jvmToolchain(21) для Kotlin, где применимо.
  - Проверить alias(...) apply false для kotlin/agp/compose/ktor/ksp/room.
- Модули:
  - :composeApp — уже указывает jvmTarget = JVM_21 (из аудита). Выровнять при необходимости с jvmToolchain(21).
  - :shared — Android compileOptions сейчас Java 11. Исправить на Java 21:
    Пример (Android):
    android {
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_21
            targetCompatibility = JavaVersion.VERSION_21
        }
    }
    Пример (Kotlin MPP):
    kotlin {
        jvmToolchain(21)
        // androidTarget { ... } / jvm() — оставить как есть, лишь выровнять toolchain
    }
  - :server — убедиться, что Kotlin JVM использует jvmToolchain(21) или эквивалентную настройку.

3) Kotlin/KSP — матрица совместимости и решение
- Текущее наблюдение из versions catalog:
  - kotlin = "2.2.21"
  - ksp = "2.3.2"
- Требование: версия KSP должна соответствовать выбранной версии Kotlin (обычно KSP строго мажорно/минорно синхронизируется с Kotlin).
- Варианты решения:
  - A (минимально-инвазивный): Понизить ksp до совместимой с Kotlin 2.2.21 (например, 2.2.21-<build>), оставить Kotlin на 2.2.21.
  - B (агрессивный): Поднять Kotlin до 2.3.2 (или актуальной совместимой с текущим ksp), затем прогнать валидизацию Compose/AGP/Room/ktor.
- Критерии выбора:
  - Стабильность сборки и минимальные изменения на Stage 0 → предпочтителен вариант A.
  - Если обнаруживаются несовместимости с Compose/AGP, рассмотреть B с тестированием.
- В рамках Stage 0:
  - Зафиксировать выбранный вариант (A по умолчанию).
  - Обновить gradle/libs.versions.toml:
    - ksp = "2.2.21" (пример; точную патч-версию зафиксировать согласно доступности плагина в проекте).
  - Убедиться, что плагин com.google.devtools.ksp применён в модулях, где нужен (shared, возможно composeApp), и подключены конфигурации:
    dependencies {
        add("kspAndroid", libs.room.compiler)
        add("kspJvm", libs.room.compiler)
    }
  - Убедиться в настройке Room схем:
    room {
        schemaDirectory("$projectDir/schemas")
    }

4) jdeps/Graphviz — контроль зависимостей (базовая валидация)
- На каждом модуле прогнать существующие задачи:
  - jdepsClassGraph, copyClassGraphToDocs, renderClassGraphSvg
  - jdepsPackageGraph
  - decorateClassGraph, renderDecoratedClassGraphSvg
- Проверить, что артефакты появляются в docs/ (или в соответствующей директории проекта, как настроено).
- Для Stage 0 не фейлить сборку на запрещённых рёбрах; пока только отчётность/визуализация и ручной аудит.
- Сохранить SVG/PNG в docs/inventory/ или docs/architecture/graphs/ (согласовать с текущими настройками тасков проекта).

5) CI/локальная валидация
- Выполнить:
  - ./gradlew clean
  - ./gradlew :shared:assemble :composeApp:assemble :server:build (или просто ./gradlew build)
  - Запустить jdeps-графы для каждого модуля.
  - Где есть тесты — ./gradlew test
- Проверка результата:
  - Сборка зелёная.
  - KSP корректно генерирует код под android/jvm.
  - jdeps-артефакты сгенерированы и скопированы в docs/.
  - Нет предупреждений о несовместимых toolchain.

6) Документация/PR
- Обновить memory-bank/progress.md — отметить завершение Stage 0 (после выполнения всех шагов).
- Подготовить краткие PR-заметки/CHANGELOG:
  - Выравнивание Java 21.
  - Синхронизация Kotlin/KSP (выбранный вариант).
  - Подтверждение работы jdeps/Graphviz и ссылки на артефакты.
  - Обновление Memory Bank.

Пример изменений в конфигурации (шаблоны)

settings.gradle.kts
- Проверить:
  enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
  // Foojay resolver:
  // pluginManagement {
  //   repositories {
  //     gradlePluginPortal()
  //     mavenCentral()
  //   }
  //   // foojay-resolver-конфигурация если используется
  // }
  include(":composeApp", ":server", ":shared")

build.gradle.kts (root)
- Убедиться, что используются алиасы с apply false:
  plugins {
      alias(libs.plugins.kotlin.multiplatform) apply false
      alias(libs.plugins.android.application) apply false
      alias(libs.plugins.android.library) apply false
      alias(libs.plugins.compose.multiplatform) apply false
      alias(libs.plugins.ktor) apply false
      alias(libs.plugins.ksp) apply false
      // ...
  }
- Общая toolchain-настройка (вариант):
  subprojects {
      plugins.withId("org.jetbrains.kotlin.jvm") {
          extensions.configure(org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension::class.java) {
              jvmToolchain(21)
          }
      }
      plugins.withId("org.jetbrains.kotlin.multiplatform") {
          extensions.configure(org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension::class.java) {
              jvmToolchain(21)
          }
      }
  }

shared/build.gradle.kts (фрагменты)
- Исправить compileOptions c 11 → 21:
  android {
      compileSdk = libs.versions.android.compileSdk.get().toInt()
      compileOptions {
          sourceCompatibility = JavaVersion.VERSION_21
          targetCompatibility = JavaVersion.VERSION_21
      }
  }
- Убедиться, что ksp подключён:
  plugins {
      // ...
      alias(libs.plugins.ksp)
  }
  dependencies {
      add("kspAndroid", libs.room.compiler)
      add("kspJvm", libs.room.compiler)
  }
  room {
      schemaDirectory("$projectDir/schemas")
  }

DoD (Definition of Done)
- Документация:
  - [ ] Stage-0-Plan.md создан и размещён в memory-bank/stage-plans/.
  - [ ] memory-bank/activeContext.md обновлён ссылками на Migration Plan и Stage-0-Plan.
  - [ ] memory-bank/progress.md отражает завершение “update memory bank (Stage 0 readiness)”.
- Toolchain:
  - [ ] Во всех модулях Android/JVM — Java 21 (compileOptions и jvmToolchain).
  - [ ] Kotlin/KSP синхронизированы (выбран и реализован вариант A или B), сборка зелёная.
- Контроль зависимостей:
  - [ ] jdeps отчёты сгенерированы для :composeApp, :shared, :server.
  - [ ] SVG-графы размещены в docs/ (или согласно текущей настройке тасков).
  - [ ] Отклонения/рискованные ребра зафиксированы в notes (если есть).
- Валидация:
  - [ ] ./gradlew build проходит без ошибок.
  - [ ] Тесты (где есть) зеленые.
- PR/Changelog:
  - [ ] Краткие заметки подготовлены (что изменено в конфигурации и почему), ссылки на артефакты.

Rollback-план
- В случае проблем с KSP/Room: вернуть предыдущую версию ksp/kotlin из versions catalog и откатить плагинные изменения, сохранить отчеты о сбое в sessions/progress.
- В случае проблем с jdeps/Graphviz: выключить рендеринг SVG (оставить только dot/текстовые отчёты), не блокировать сборку.

Метрики успешности Stage 0
- Build Success Rate: 100% по локальной сборке (все модули).
- KSP Generation: 100% успешной генерации для android/jvm в :shared (и других, где применяется).
- Отчётность: Графы зависимостей присутствуют и читаемы; нет критических циклов между слоями.
- Документация: Memory Bank (activeContext, progress) синхронизирован; ссылки работают.

Следующий этап (предварительно)
- Stage 1: Введение базовых модулей оболочек (frontend:core, shared:core, shared:contracts) и перенос минимального общего кода/контрактов без изменения функционала, включение мягких проверок зависимостей (warning-level).
