# Документация AIChallenge — INDEX

Этот индекс агрегирует навигацию по документации проекта для людей и LLM. Следуйте порядку чтения для быстрого понимания архитектуры и правил разработки. Для автоматической генерации кода LLM используйте раздел “LLM-набор”.

Содержание:
- Быстрый старт
- Документация для людей (Human)
- Набор для LLM (LLM)
- Инвентаризация проекта
- Матрица соответствия требованиям
- Поддержка и обновление документации

## Быстрый старт

1) Ознакомьтесь с архитектурой и структурой:
- docs/human/Architecture.md
- docs/human/ProjectStructure.md

2) Примите правила кодирования и внесения изменений:
- docs/human/CodingStandards.md
- docs/human/Contributing.md

3) Изучите процесс разработки фич:
- docs/human/FeatureDevelopmentGuide.md

4) Внимание на системные политики:
- docs/human/ErrorHandling.md
- docs/human/API-Networking.md
- docs/human/Data-Persistence.md
- docs/human/Logging-Metrics.md
- docs/human/Security.md
- docs/human/TestingStrategy.md

## Документация для людей (Human)

Архитектура и структура:
- Архитектура: docs/human/Architecture.md
- Структура проекта: docs/human/ProjectStructure.md

Правила и процессы:
- Правила написания кода: docs/human/CodingStandards.md
- Руководство по разработке фич: docs/human/FeatureDevelopmentGuide.md
- Вклад в проект (Contributing): docs/human/Contributing.md

Системные политики:
- API и сеть (Ktor): docs/human/API-Networking.md
- Данные и персистентность: docs/human/Data-Persistence.md
- Обработка ошибок: docs/human/ErrorHandling.md
- Логирование и метрики: docs/human/Logging-Metrics.md
- Безопасность и секреты: docs/human/Security.md
- Стратегия тестирования: docs/human/TestingStrategy.md

## Набор для LLM (LLM)

Ядро:
- System Prompt: docs/llm/system_prompt.md
- Правила генерации кода: docs/llm/code_generation_rules.md
- Политика путей и структуры: docs/llm/file_paths_policy.md
- Правила поддержки LLM-набора: docs/llm/maintenance_rules.md

Knowledge (короткие сводки):
- Архитектура (сводка): docs/llm/knowledge/architecture.md
- Модули и фичи: docs/llm/knowledge/modules.md
- Данные и модели: docs/llm/knowledge/data_models.md

Templates (минимальные шаблоны):
- FeatureViewModel (MVI): docs/llm/templates/FeatureViewModel.kt.md
- FeatureModule (Koin): docs/llm/templates/FeatureModule.kt.md
- UseCase (интерфейс/Impl): docs/llm/templates/UseCase.kt.md

## Инвентаризация проекта

- Полная инвентаризация ключевых файлов: docs/inventory/project_inventory.json

Используйте этот файл, чтобы сверять пути и имена файлов в документации и LLM-шаблонах.

## Матрица соответствия требованиям

- Архитектура проекта:
  - Human: docs/human/Architecture.md
  - LLM: docs/llm/knowledge/architecture.md, docs/llm/system_prompt.md

- Структура проекта:
  - Human: docs/human/ProjectStructure.md
  - LLM: docs/llm/file_paths_policy.md, docs/llm/knowledge/modules.md

- Правила написания кода:
  - Human: docs/human/CodingStandards.md
  - LLM: docs/llm/code_generation_rules.md

- Правила внедрения нового функционала:
  - Human: docs/human/FeatureDevelopmentGuide.md
  - LLM: docs/llm/templates/*.kt.md (скелеты), docs/llm/file_paths_policy.md

- Дополнительная документация:
  - ErrorHandling: docs/human/ErrorHandling.md
  - API-Networking: docs/human/API-Networking.md
  - Data-Persistence: docs/human/Data-Persistence.md
  - Logging-Metrics: docs/human/Logging-Metrics.md
  - Security: docs/human/Security.md
  - TestingStrategy: docs/human/TestingStrategy.md
  - Inventory: docs/inventory/project_inventory.json
  - Maintenance (LLM): docs/llm/maintenance_rules.md

## Поддержка и обновление документации

- При добавлении фич/модулей:
  - Обновить docs/inventory/project_inventory.json.
  - Актуализировать ProjectStructure.md и LLM-knowledge (modules.md).
- При изменении архитектурных принципов:
  - Обновить Architecture.md и LLM-knowledge (architecture.md).
  - При необходимости — code_generation_rules.md и templates/.
- При изменении моделей данных:
  - Обновить Data-Persistence.md и LLM-knowledge (data_models.md).
- После добавления документов — обязательно добавить/обновить ссылки в этом INDEX.
