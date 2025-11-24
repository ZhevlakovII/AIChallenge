# Stage 1 — Multimodule KMP: ядро (core) и контроль зависимостей

Статус: Draft  
Дата: 2025-11-22  
Автор: Cline (по ADR-2025-11-21)  
Ссылки:  
- ADR: [../decisions/ADR-2025-11-21-мультимодульная-миграция-KMP.md](../decisions/ADR-2025-11-21-мультимодульная-миграция-KMP.md)  
- Active Context: [../activeContext.md](../activeContext.md)  
- Migration Plan: [../migration-plan.md](../migration-plan.md)  
- Stage 0 Plan: [Stage-0-Plan.md](Stage-0-Plan.md)

---

1) Scope / Outcomes (результаты Stage 1)
- Сформирована начальная топология ядра:
  - :shared:core — общие доменные модели, типы, абстракции, константы, базовые утилиты без платформенных деталей.
  - :frontend:core — общие фронтенд-контракты/интерфейсы (без UI-реализаций), навигационные типы, модельные контракты.
  - :backend:core — ядро для серверной части (JVM-only): общие контракты сервисов, DTO, use-case границы.
- Границы DI совпадают с границами Gradle-модулей. Никаких горизонтальных зависимостей между модулями одного уровня.
- Интегрирован контроль зависимостей (jdeps + Graphviz) для новых модулей:
  - summary.dot, package/summary.dot, summary.decorated.dot, violations.txt для каждого модуля.
  - Отчеты генерируются, но не ломают сборку (build still green). Строгое «fail on violations» включим на Stage ≥ 3 по ADR.
- Сборка и тесты зелёные (включая :shared, :server), Android Lint проходит.
- Документация и трассируемость: Active Context обновлен; добавлены записи в progress.md и sessions/.

2) Инварианты зависимостей (из ADR)
- Запрещены горизонтальные зависимости между модулями одного слоя.
- «Вверх» по слоям запрещено; «вниз» — разрешено по белому списку.
- DI-модуль (Koin) не должен пробрасывать зависимости, нарушающие границы слоёв.
- Конвенция артефактов jdeps/Graphviz:
  - docs/architecture/<module>/summary.dot
  - docs/architecture/<module>/package/summary.dot
  - docs/architecture/<module>/summary.decorated.dot
  - docs/architecture/<module>/violations.txt
- Строгий режим (fail on violations) включается на более позднем этапе (Stage 3), сейчас — только отчетность.

Разрешённая матрица (на Stage 1):
- :shared:core → (нет внешних модулей внутри проекта; только сторонние либы через versions catalog)
- :frontend:core → :shared:core
- :backend:core → :shared:core

3) Gradle/Tooling конвенции (напоминание)
- Java Toolchain 21.
- Versions catalog (gradle/libs.versions.toml); ksp = "2.3.2" подтверждена.
- TYPESAFE_PROJECT_ACCESSORS = true.
- Foojay Toolchains Resolver.
- Kotlin Multiplatform:
  - :shared:core — KMP (commonMain + androidMain; при необходимости jvmMain для реиспользования).
  - :frontend:core — KMP (commonMain + androidMain). UI-фреймворки (Compose) на этом этапе не подключаем, только контракты.
  - :backend:core — JVM target (JVM-only).
- Room KMP — остаётся в :shared (на этом этапе не выносим); KSP per target настроен в существующих модулях.
- Koin для DI — объявление модулей строго на границах Gradle-модулей.
- Android Lint — quality gate.

4) План работ (пошагово)

Шаг 1. Подготовка top-level структуры
- В settings.gradle.kts включить:
  - include(":shared:core")
  - include(":frontend:core")
  - include(":backend:core")
- Убедиться, что TYPESAFE_PROJECT_ACCESSORS включен.

Шаг 2. Создать модули ядра
- :shared:core — минимальный KMP-библиотечный модуль с общими доменными типами/константами/утилитами.
- :frontend:core — контракты для слоя представления/навигации без зависимостей на UI-реализации.
- :backend:core — JVM-библиотека для серверного доменного ядра (контракты/DTO/use-case границы).

Шаг 3. Базовая конфигурация Gradle
- Пример (shared:core/build.gradle.kts):
```kotlin
plugins {
    kotlin("multiplatform")
    id("com.android.library")
}

kotlin {
    androidTarget()
    jvm() // опционально, если требуется совместное использование
    sourceSets {
        val commonMain by getting {
            dependencies {
                // kotlin stdlib, kotlinx.serialization (если нужны), koin-core (при необходимости DI-контрактов)
            }
        }
        val androidMain by getting {
            dependencies {
                // androidX core-ktx, если потребуется
            }
        }
        val commonTest by getting
        val androidUnitTest by getting
    }
}

android {
    namespace = "ru.izhxx.aichallenge.shared.core"
    compileSdk = 35
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}
```

- Пример (frontend:core/build.gradle.kts):
```kotlin
plugins {
    kotlin("multiplatform")
    id("com.android.library")
}

kotlin {
    androidTarget()
    sourceSets {
        val commonMain by getting {
            dependencies {
                // api(projects.shared.core) — typesafe accessors
                api(projects.shared.core)
            }
        }
        val androidMain by getting
        val commonTest by getting
    }
}

android {
    namespace = "ru.izhxx.aichallenge.frontend.core"
    compileSdk = 35
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}
```

- Пример (backend:core/build.gradle.kts):
```kotlin
plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    api(projects.shared.core)
    // kotlinx.serialization-core / ktor-utils (по необходимости)
    testImplementation(kotlin("test"))
}
```

Шаг 4. DI-скелет
- В каждом модуле создать пакет di/ с абстрактными Koin-модулями (без связки на реализации других слоев).
- Никаких «сквозных» биндингов через границы модулей.

Шаг 5. Минимальная миграция кода
- Перенести в :shared:core безопасные для разделения элементы:
  - Constants, базовые Value Objects, sealed-интерфейсы контрактов, простые Result-типы.
- В :frontend:core — только интерфейсы навигации/контрактов представления (без зависимостей на Compose/Android UI).
- В :backend:core — общие контракты домена/DTO/use-case границы (JVM).
- Обновить импорты в существующем коде (:shared, :server, composeApp) строго по разрешённым зависимостям.

Шаг 6. Интеграция jdeps/Graphviz (для каждого нового модуля)
- Добавить Gradle-задачи по аналогии с :shared:
  - jdepsClassGraph → copyClassGraphToDocs → renderClassGraphSvg
  - jdepsPackageGraph → copyPackageGraphToDocs
  - decorateClassGraph → renderDecoratedClassGraphSvg
- Выходы складывать в docs/architecture/<module>/ (см. инварианты).
- На Stage 1 не «проваливаем» сборку при нарушениях, но письменно фиксируем предупреждения в violations.txt и progress.md.

Шаг 7. CI и quality gates
- В CI: собрать новые модули, прогнать тесты, сгенерировать jdeps-артефакты, артефакты загрузить как build artifacts.
- Android Lint — не деградировать метрики по сравнению со Stage 0.

Шаг 8. Трассируемость
- Обновить memory-bank:
  - activeContext.md — ссылки на Stage-1-Plan.
  - progress.md — запись о создании модулей/артефактов.
  - sessions/ — заметка сессии с решением/рисками.

5) DoD (Definition of Done) для Stage 1
- [ ] В settings.gradle.kts подключены :shared:core, :frontend:core, :backend:core.
- [ ] Модули созданы, компилируются на Java 21/KMP, соблюдены versions catalog; ksp остаётся 2.3.2 (подтверждено).
- [ ] Сборка/тесты зелёные (включая существующие :shared, :server).
- [ ] Для каждого модуля сгенерированы: summary.dot, package/summary.dot, summary.decorated.dot, violations.txt.
- [ ] В violations.txt нет горизонтальных зависимостей; потенциальные риски зафиксированы.
- [ ] Active Context обновлён; добавлена ссылка на Stage-1-Plan; обновлены progress.md и sessions/.
- [ ] Документированы риски и компромиссы.

6) Риски и смягчения
- Риск: ранняя фрагментация кода ухудшит скорость разработки.
  - Смягчение: переносить минимально необходимое; валидировать jdeps-графы на каждом шаге.
- Риск: скрытые платформенные зависимости попадут в :shared:core.
  - Смягчение: ревью PR с проверкой sourceSets; автоматическая проверка package-графа.
- Риск: пересборки с KSP per target увеличат время CI.
  - Смягчение: кэширование, ограничение KSP на модулях, где это оправдано.

7) Rollback Plan
- Откат добавления новых модулей возможен отдельным PR, при этом код возвращается в исходные модули (:shared, composeApp, :server).
- Jdeps-артефакты — диагностические, не блокируют откат.

8) Дорожная карта вперёд (Stage 2+ предварительно)
- Stage 2: Выделение :shared:contracts, подготовка API-границ для фичей, старт разбиения на :frontend:features:<feature>:{api,impl}.
- Stage 3: Включение «fail on violations» для ключевых модулей.
- Stage 4–5: Полное разбиение фич, стабилизация интерфейсов, оптимизация сборок и CI.

Примечания по реализации:
- Использовать typesafe accessors (projects.shared.core и т.п.).
- Сохранять нейтральность к UI/Compose на уровне :frontend:core (только контракты).
- Не выносить Room KMP на Stage 1; оставить его в существующей конфигурации :shared до утверждения границ данных на следующих этапах.
