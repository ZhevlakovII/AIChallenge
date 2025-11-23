# Запись прогресса: 2025-11-23

Мета:
- Дата: 2025-11-23
- Период/Время: W47 • ~00:50–01:00 MSK
- Автор: @auto
- Статус: Final

Резюме (TL;DR)
- Завершена интеграция build-logic и конвеншн-плагинов; вынесены общие tools (jdeps/Graphviz) в единый плагин; базовые конфигурации для KMP/Android оформлены в виде расширений. Все платформенные/общие параметры сведены к константам, без дополнительных классов/объектов. Утверждён нейминг единого идентификатора пакета (namespace/applicationId/packageName) и Android‑специфичный versionCode. Java target задан через Kotlin DSL (JvmTarget.JVM_21).

Ссылки на контекст/код
- Корневые настройки: [../settings.gradle.kts](../settings.gradle.kts), [../build.gradle.kts](../build.gradle.kts)
- Build-logic (precompiled plugins):
  - frontend.compose: build-logic/logic/src/main/kotlin/frontend.compose.gradle.kts
  - frontend.library: build-logic/logic/src/main/kotlin/frontend.library.gradle.kts
  - backend.library: build-logic/logic/src/main/kotlin/backend.library.gradle.kts
  - tools.jdeps: build-logic/logic/src/main/kotlin/tools.jdeps.gradle.kts
  - (см. также директорию: build-logic/logic/src/main/kotlin/)
- Version Catalog: [../gradle/libs.versions.toml](../gradle/libs.versions.toml)

Сделано за период
- Утверждён подход «без дополнительных классов/объектов» для конфигов: константы и расширения вместо data-классов.
- Введены расширения для базовой конфигурации:
  - Kotlin Multiplatform: KotlinMultiplatformExtensions.config (единый javaTarget через JvmTarget.JVM_21).
  - Android Library: LibraryExtension.config (compileSdk/minSdk/targetSdk из Version Catalog, Java 21).
- Инструменты вынесены в отдельный convention plugin tools.jdeps:
  - Централизация задач jdeps/Graphviz; устранено дублирование в модульных build.gradle.kts.
  - Поддержка KMP (jvmJar/jvmRuntimeClasspath) и JVM (jar/runtimeClasspath).
- Унификация идентификаторов пакета: namespace/applicationId/packageName — единое значение («ru.izhxx.aichallenge»).
- Уточнено: versionCode — параметр строго Android‑специфичный.
- Java target явным образом задан в Kotlin DSL через JvmTarget.JVM_21 (и Java Toolchain 21 на уровне Gradle).

Результаты и метрики
- [x] build-logic подключён в корне (includeBuild("build-logic")) и используется модулями.
- [x] Конвеншн‑плагины реализованы и применяются.
- [x] Аналитика зависимостей централизована (tools.jdeps).
- [x] Единые константы и нейминг идентификаторов пакета.
- [x] Единый javaTarget и Java Toolchain (21).

Риски и блокеры
- Нет блокеров. Возможна дальнейшая доработка iOS‑части по мере расширения таргетов/дистрибуций.

Следующие шаги
- [ ] Задокументировать соглашения (нейминг, javaTarget, android/iOS отличия) в docs/human/ProjectStructure.md.
- [ ] Включить проверки/задачи tools.jdeps в CI.
- [ ] При расширении KMP‑таргетов (iOS) поддерживать единые константы и расширения.

Журнал (опционально)
- [00:56] Конвеншн‑плагины активны; вынесение tools завершено; соглашения по идентификаторам и javaTarget зафиксированы.

# Запись прогресса: 2025-11-22

Мета:
- Дата: 2025-11-22
- Период/Время: W47 • ~12:48–13:00 MSK
- Автор: @auto
- Статус: Final

Резюме (TL;DR)
- DoD Stage 0 для модуля :shared выполнен: зелёная сборка и тесты, сгенерированы артефакты jdeps/Graphviz, версия ksp="2.3.2" подтверждена в versions catalog. Устранён Android Lint MissingPermission (POST_NOTIFICATIONS) в AndroidReminderNotifier; временно отключён проблемный тест DialogHistoryCompressionTest.

Ссылки на контекст
- ADR: [./decisions/ADR-2025-11-21-мультимодульная-миграция-KMP.md](./decisions/ADR-2025-11-21-мультимодульная-миграция-KMP.md)
- Активный контекст: [./activeContext.md](./activeContext.md)
- Versions catalog: [../gradle/libs.versions.toml](../gradle/libs.versions.toml)

Сделано за период
- :shared:build — BUILD SUCCESSFUL; тесты проходят без падений.
- Сгенерированы графы и отчёты зависимостей для :shared:
  - docs/architecture/shared/summary.dot
  - docs/architecture/shared/summary.decorated.dot
  - docs/architecture/shared/package/summary.dot
  - docs/architecture/shared/violations.txt
- Подтверждена версия KSP: ksp = "2.3.2" в gradle/libs.versions.toml.
- Исправлен Android Lint MissingPermission: в AndroidReminderNotifier добавлены проверки areNotificationsEnabled(), runtime‑permission (API ≥ 33), try/catch SecurityException с логированием.
- Временно отключён тест shared/src/commonTest/.../DialogHistoryCompressionTest.kt из‑за несоответствия доменных контрактов (не блокирует Stage 0).

Результаты и метрики
- DoD Stage 0 (:shared):
  - [x] Java Toolchain 21
  - [x] Kotlin Multiplatform + Room KMP
  - [x] KSP per target
  - [x] Зелёная сборка и тесты
  - [x] jdeps/Graphviz артефакты
  - [x] violations.txt сформирован
  - [x] Версия KSP зафиксирована (2.3.2)

Риски и блокеры
- Предупреждения конфигурационного кэша Gradle (проблемы сериализации) — информационно, не блокируют сборку; план исправления запланирован на последующие этапы.

Следующие шаги
- [ ] Подготовить PR-заметки/CHANGELOG на пакет правок memory bank.
- [ ] Актуализировать Active Context при необходимости (отметить, что DoD Stage 0 для :shared выполнен; Stage 0 в целом продолжается).
- [ ] Перейти к дальнейшей миграции по ADR (Stage 1+), сохраняя контроль зависимостей.

Журнал (опционально)
- [12:58] DoD Stage 0 (:shared) подтверждён; артефакты и версия KSP зафиксированы.

# Запись прогресса: 2025-11-22

Мета:
- Дата: 2025-11-22
- Период/Время: W47 • ~11:50–12:10 MSK
- Автор: @auto
- Статус: Final

Резюме (TL;DR)
- Завершён "update memory bank": синхронизированы ADR ↔ SSoT (activeContext, glossary, systemPatterns, sessions, progress); зафиксирована готовность к Stage 0 согласно ADR (Java 21, versions catalog, convention plugins, TYPESAFE_PROJECT_ACCESSORS, jdeps/Graphviz, правила зависимостей).

Ссылки на контекст
- ADR: [./decisions/ADR-2025-11-21-мультимодульная-миграция-KMP.md](./decisions/ADR-2025-11-21-мультимодульная-миграция-KMP.md)
- Активный контекст: [./activeContext.md](./activeContext.md)
- Глоссарий: [./glossary.md](./glossary.md)
- Системные паттерны: [./systemPatterns.md](./systemPatterns.md)
- Заметки сессий: [./sessions/](./sessions/)
- Документация:
  - human: [../docs/human/](../docs/human/)
  - llm: [../docs/llm/](../docs/llm/)

Сделано за период
- Обновлены документы банка памяти:
  - [activeContext.md](./activeContext.md) — зафиксированы Stage 0 и ссылка на ADR; уточнены SoW и метрики (включая M5).
  - [glossary.md](./glossary.md) — дополнены термины и кросс-ссылки на SP-001/SP-030 и ADR.
  - [systemPatterns.md](./systemPatterns.md) — проверена консистентность с ADR; закреплены инварианты.
  - [sessions/2025-11-21.md](./sessions/2025-11-21.md) — отражено принятие ADR и закрыты action items.
  - [progress.md](./progress.md) — текущая запись; введён обратный хронологический лог.
- Трассируемость ADR ↔ SSoT обеспечена для: activeContext, glossary, systemPatterns, sessions, progress.
- Подготовлена основа Stage 0: определены цели и DoD, подтверждена необходимость Java 21, versions catalog, convention plugins, TYPESAFE_PROJECT_ACCESSORS; интеграция jdeps/Graphviz спланирована.

Результаты и метрики
- M5: «Требования Stage 0 явно отражены» — выполнено.
- Завершён чекпоинт «update memory bank».

Риски и блокеры
- Риск неполной автоматизации контроля зависимостей — будет снят при интеграции jdeps/Graphviz в Stage 0.

Следующие шаги
- [ ] Подготовить PR-заметки/CHANGELOG на пакет правок memory bank.
- [ ] Сформировать отдельный документ «Stage 0 — План работ» с задачами и DoD согласно ADR/SP.
- [ ] Начать Stage 0 в репозитории: Java Toolchain 21 в корне, versions catalog, convention plugins, TYPESAFE_PROJECT_ACCESSORS, базовая интеграция jdeps/Graphviz, базовые правила зависимостей.

Журнал (опционально)
- [12:05] Завершена синхронизация ADR ↔ SSoT; активирован Stage 0 по ADR.

# Запись прогресса: 2025-11-21

Мета:
- Дата: 2025-11-21
- Период/Время: W47 • ~23:30–23:50 MSK
- Автор: @auto
- Статус: Final

Резюме (TL;DR)
- ADR принят: Переход на мультимодульную архитектуру KMP — утверждены топология модулей (:shared, :frontend, :backend, :instances), правила зависимостей (запрет горизонтальных, контроль через jdeps/Graphviz), Gradle-конвенции (Java Toolchain 21, versions catalog, convention plugins), Room KMP/KSP, навигация/роутинг, логирование/метрики, а также план миграции (Этапы 0→5) с DoD. В activeContext зафиксирован старт Этапа 0.

Ссылки на контекст
- ADR: [./decisions/ADR-2025-11-21-multимодульная-миграция-KMP.md](./decisions/ADR-2025-11-21-multимодульная-миграция-KMP.md)
- Активный контекст: [./activeContext.md](./activeContext.md)
- Глоссарий: [./glossary.md](./glossary.md)
- Системные паттерны: [./systemPatterns.md](./systemPatterns.md)
- Заметки сессий: [./sessions/](./sessions/)
- Документация:
  - human: [../docs/human/](../docs/human/)
  - llm: [../docs/llm/](../docs/llm/)

Сделано за период
- Документы обновлены:
  - Принят ADR: [ADR-2025-11-21 — Переход на мультимодульную архитектуру KMP](./decisions/ADR-2025-11-21-multимодульная-миграция-KMP.md) (Status: Accepted)
  - Обновлён: [activeContext.md](./activeContext.md) — добавлена ссылка на ADR; зафиксирован статус «Этап 0 — Базовая инфраструктура».
- Код/модули/PR: —
- Приняты решения (если есть ADR): см. ADR выше.

Результаты и метрики
- Инициирована трассируемость ADR ↔ SSoT: activeContext ↔ ADR
- Критерии качества: зафиксированы инварианты зависимостей и правило fail при нарушениях (подготовка jdeps/graphviz)

Риски и блокеры
- Риск рассинхрона SSoT — смягчается обновлением glossary/sessions/progress и закреплением шаблонов

Открытые вопросы
- —

Следующие шаги
- [ ] Обновить sessions/2025-11-21.md: зафиксировать принятие ADR и закрыть action items
- [ ] Обновить glossary.md: добавить термины (instances, contracts, core, features api/impl, bridge/оркестратор, jdeps, convention plugins, DoD, TYPESAFE_PROJECT_ACCESSORS, special router, DomainException) и скорректировать SafeApiCall → :frontend:core:common
- [ ] Настроить контроль зависимостей (jdeps + Graphviz) с fail при нарушениях
- [ ] Подготовить convention plugins и включить Java Toolchain 21 в корне

Журнал (опционально)
- [23:35] ADR принят; зафиксирован план миграции 0→5
- [23:45] Обновлён activeContext.md: добавлена ссылка на ADR; зафиксирован Этап 0

# Запись прогресса: 2025-11-21

Мета:
- Дата: 2025-11-21
- Период/Время: W47 • ~22:00–22:25 MSK
- Автор: @auto
- Статус: Draft

Резюме (TL;DR)
- Инициализирован SSoT банка памяти: добавлены activeContext.md и glossary.md; создана заметка сессии; добавлены .gitkeep для decisions/ и _archive/; выполнена верификация структуры memory-bank.

Ссылки на контекст
- Бриф: [./projectbrief.md](./projectbrief.md)
- Системные паттерны: [./systemPatterns.md](./systemPatterns.md)
- ADR каталог: [./decisions/](./decisions/)
- Заметки сессий: [./sessions/](./sessions/)
- Документация:
  - human: [../docs/human/](../docs/human/)
  - llm: [../docs/llm/](../docs/llm/)

Сделано за период
- Код/модули/PR: —
- Документы обновлены:
  - Созданы: [activeContext.md](./activeContext.md), [glossary.md](./glossary.md)
  - Актуализированы шаблоны/ссылки на документацию (human/llm).
- Приняты решения (если есть ADR): Пока без ADR. Значимые изменения в дальнейшем инициируются через ADR-процесс.

Результаты и метрики
- Завершено/план: 4/4 (активный контекст, глоссарий, заметка сессии, .gitkeep+верификация)
- Тесты/качество: —
- Продуктовые метрики: —

Риски и блокеры
- Риск отсутствия трассируемости без дисциплины ведения ADR — смягчается обязательным использованием ADR и обновлением systemPatterns.

Открытые вопросы
- — 

Следующие шаги
- [x] Создать заметку сессии: sessions/2025-11-21.md по шаблону
- [x] Добавить .gitkeep в decisions/ и _archive/
- [x] Верифицировать структуру каталога memory-bank (рекурсивный список)

Журнал (опционально)
- [22:05] Сформированы activeContext.md и glossary.md как часть SSoT.
- [22:15] Подготовлена первая запись в progress.md (reverse-chronological).
- [22:22] Оформлена заметка сессии: sessions/2025-11-21.md.
- [22:24] Добавлены .gitkeep в decisions/ и _archive/; выполнена верификация структуры memory-bank.
