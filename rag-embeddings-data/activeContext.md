# Active Context — оперативный контекст проекта

Обновлено: 2025-11-22  
Язык: RU  
Статус: Active

Связанные документы:
- Product: [productContext.md](./productContext.md)
- Tech: [techContext.md](./techContext.md)
- Паттерны: [systemPatterns.md](./systemPatterns.md)
- Решения (ADR): [decisions/](./decisions/)
- Сессии: [sessions/](./sessions/)
- Прогресс: [progress.md](./progress.md)
- Project Brief: [../memory-bank/projectbrief.md](./projectbrief.md)
- Руководства: [../docs/human/](../docs/human/) • Политики LLM: [../docs/llm/](../docs/llm/)
- План миграции: [migration-plan.md](./migration-plan.md)
- План Stage 0: [stage-plans/Stage-0-Plan.md](./stage-plans/Stage-0-Plan.md)
- План Stage 1: [stage-plans/Stage-1-Plan.md](./stage-plans/Stage-1-Plan.md)
- ADR принят: [ADR-2025-11-21 — Переход на мультимодульную архитектуру KMP](./decisions/ADR-2025-11-21-мультимодульная-миграция-KMP.md)

---

1) TL;DR
- Цель сейчас: завершить инициализацию банка памяти (SSoT) и ввести регламентный цикл ADR → systemPatterns → sessions/progress.
- Статус: ADR Accepted (2025-11-21) — выполняется Этап 0 «Базовая инфраструктура» (см. ссылку на ADR выше).
- Объем Stage 0: Java Toolchain 21; versions catalog; convention plugins; TYPESAFE_PROJECT_ACCESSORS; интеграция jdeps/Graphviz; правила зависимостей (build fail при нарушениях после DoD Stage 3).
- Критерий готовности: все базовые файлы инициализированы; первая запись в progress.md; создана заметка сессии; заведены каталоги decisions/ и _archive/ с .gitkeep; проверена структура; кросс-ссылки ADR ↔ SSoT проставлены.
- Риск: неполная трассируемость без стартовой записи progress и сессии.
- Промежуточный результат: DoD Stage 0 для модуля :shared выполнен — сборка и тесты зелёные; Android Lint проходит; артефакты зависимостей сгенерированы (docs/architecture/shared/summary.dot, summary.decorated.dot, package/summary.dot, violations.txt); версия ksp=2.3.2 подтверждена.

2) Текущий фокус и приоритеты
- ФОКУС: Stage 0 — Базовая инфраструктура (см. [Stage-0-Plan](./stage-plans/Stage-0-Plan.md), [Migration Plan](./migration-plan.md))
- P1: Создать базовые документы и шаблоны, завершить стартовую инициализацию.
- P1: Обеспечить трассируемость: ADR ↔ [systemPatterns.md](./systemPatterns.md) ↔ [sessions/](./sessions/) ↔ [progress.md](./progress.md).
- P2: Сверить ссылки с [../docs/human/](../docs/human/) и [../docs/llm/](../docs/llm/).
- P1: Подготовка к Stage 1 согласно ADR-2025-11-21 (мультимодульная миграция KMP).

3) Активные задачи (SoW)
- Stage 0 продолжается для инфраструктуры проекта; при этом DoD Stage 0 для :shared выполнен.
- Подготовить старт Stage 1 согласно ADR-2025-11-21: разбиение на модули по слоям/фичам, границы DI = границы Gradle‑модулей, настройка задач jdeps/Graphviz для новых модулей.
- Поддерживать контроль зависимостей в CI: генерация и проверка summary.dot, summary.decorated.dot, package/summary.dot и violations.txt.
- Подготовить PR-заметки/CHANGELOG для пакета правок memory bank.

4) Ограничения и договорённости
- Язык — RU; даты — YYYY-MM-DD.
- progress.md — обратная хронология (новое сверху).
- ADR: Proposed → Accepted/Rejected → Superseded; при изменениях обновлять [systemPatterns.md](./systemPatterns.md).
- Имя ADR: YYYYMMDD-<slug>.md (см. шаблон: [./_templates/ADR-template.md](./_templates/ADR-template.md)).

5) Метрики успеха (инкрементальные)
- M1: Созданы activeContext.md, glossary.md, progress.md (с первой записью), sessions/2025-11-21.md.
- M2: Присутствуют decisions/.gitkeep и _archive/.gitkeep.
- M3: Все ссылки из активного контекста валидны.
- M4: Трассируемость зафиксирована в progress.md и сессии.
- M5: Требования Stage 0 явно отражены в activeContext.md.

6) Зависимости и риски
- Зависимости: корректность ссылок на [productContext.md](./productContext.md), [techContext.md](./techContext.md), [systemPatterns.md](./systemPatterns.md), каталоги decisions/, sessions/, _archive/.
- Риски: расхождение регламентов; потеря истории решений без ADR.

7) Процесс изменений и трассируемость
- Любое значимое изменение инициировать через ADR (см. [./_templates/ADR-template.md](./_templates/ADR-template.md)).
- При принятии ADR обновить связанные разделы в [systemPatterns.md](./systemPatterns.md).
- Все сдвиги фиксировать в [sessions/](./sessions/) и [progress.md](./progress.md) (обратная хронология).
- При устаревании — помечать Superseded и ссылаться на заменяющий ADR.

8) Источники контекста
- Руководства команды: [../docs/human/](../docs/human/), политики LLM: [../docs/llm/](../docs/llm/), Project Brief: [./projectbrief.md](./projectbrief.md).
- Модули системы: см. [techContext.md](./techContext.md) (composeApp/shared/server).

9) История изменений
- 2025-11-22: Зафиксирован промежуточный результат — DoD Stage 0 для :shared выполнен; добавлены указания на артефакты и подготовку к Stage 1.
- 2025-11-22: Добавлены ссылки на Migration Plan и Stage-0-Plan; установлен явный фокус Stage 0 в разделе 2.
- 2025-11-22: Обновлён TL;DR (объем Stage 0), исправлена ссылка на ADR, обновлён раздел SoW, добавлены метрики.
- 2025-11-21: Создан документ activeContext.md. Инициирована финализация стартовой инициализации банка памяти (SSoT).
