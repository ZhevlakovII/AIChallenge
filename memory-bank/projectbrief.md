# Банк памяти DevX Agent — projectbrief.md (AIChallenge)

Я DevX Agent, инженер-программист с «нулевой» памятью между сессиями. Банк памяти — мой источник истины. Этот документ задаёт рамки проекта AIChallenge, определяет цели, объём и связь с остальными файлами банка памяти.

## 1) Назначение и объём документа
- Формирует основы: зачем существует проект, что именно строим, основные сценарии, архитектурные рамки, ограничения, риски и критерии успеха.
- Определяет связи с файлами productContext.md, systemPatterns.md, techContext.md, activeContext.md и progress.md.
- Служит отправной точкой для разработки, онбординга и восстановления контекста после «сброса памяти».

## 2) Краткое описание проекта
AIChallenge — Kotlin Multiplatform проект, который включает:
- Клиентское приложение на Compose Multiplatform (Android, Desktop/JVM) с фичами:
  - Chat (чат с LLM, маршрутизация в разные провайдеры через репозитории и use case),
  - Metrics (экран метрик/логов),
  - MCP (просмотр/вызов инструментов),
  - Reminder (задачи-напоминания),
  - Settings (настройки LLM/провайдера).
- Общий модуль shared с Domain/Data/DI, реализующий чистую архитектуру, Koin, Ktor-клиент, DataStore/БД, мапперы DTO↔Domain, SafeApiCall.
- Ktor-сервер с WebSocket-эндпоинтом MCP (JSON-RPC 2.0) и набором инструментов (health_check, get_time, get_rub_usd_rate, echo, sum, GitHub-интеграции).

Основные точки входа:
- UI: composeApp/src/commonMain/kotlin/ru/izhxx/aichallenge/App.kt (навигация, нижняя панель, экраны фич).
- DI (shared): shared/src/commonMain/kotlin/ru/izhxx/aichallenge/di/SharedModule.kt (HttpClient, OpenAIApi, репозитории, use case, ReminderEngine, include модулей parsers/compression/metrics/mcp).
- Server: server/src/main/kotlin/ru/izhxx/aichallenge/Application.kt (Netty, WS /mcp, GET /, инструменты MCP).

## 3) Почему проект существует (Product intent)
- Цель: ускорить разработку и экспериментирование с LLM-клиентом и MCP-инструментами в локальной среде, обеспечив строгую архитектуру и воспроизводимость.
- Решаемые проблемы:
  - Единая точка доступа к LLM (через Domain-контракты и Data-реализации).
  - Локальная интеграция инструментов (MCP WS) для расширения возможностей агента.
  - Прозрачная аналитика/метрики и воспроизводимый DI.
- Пользовательский опыт:
  - Простой UX: нижняя навигация, быстрый доступ к чату/метрикам/инструментам/настройкам.
  - Надёжные сообщения об ошибках (нормализованные в DomainException).
  - Конфигурации (провайдер/параметры LLM) — через Settings с персистентностью.

## 4) Цели и границы (Scope / Non-goals)
- MVP цели:
  - Отправка сообщений в чат и получение ответов LLM через LLMClientRepository.
  - Просмотр базовых метрик/логов (экран Metrics).
  - Взаимодействие с локальным MCP по WS: список инструментов и вызовы.
  - Напоминания (планирование/выполнение задач) через ReminderEngine.
  - Конфигурирование провайдера и параметров LLM (DataStore).
- Вне объёма (на текущем этапе):
  - Продвинутые сценарии агентности (многошаговое планирование, долгоживущие сессии).
  - Оркестрация инструментов с контекстом нескольких агентов.
  - Продакшн-хостинг и облачная инфраструктура.

## 5) Архитектура и паттерны (высокоуровнево)
- Чистая архитектура (DIP): Presentation зависит от Domain-контрактов; Data реализует контракты и скрывает DTO.
- MVI в UI: State (immut), Event (sealed), ViewModel с StateFlow; однонаправленный поток данных.
- DI: Koin модули в shared и фичевые DI-модули в composeApp.
- Сеть: Ktor HttpClient (+ContentNegotiation, WebSockets, HttpTimeout), Kotlinx.serialization.
- Безопасность/качество: SafeApiCall, нормализация ошибок, логирование, маскирование секретов.
- KMP: максимум логики в commonMain; платформенная специфика — в androidMain/jvmMain.

Связанные подробности: docs/human/Architecture.md, docs/llm/code_generation_rules.md, docs/llm/file_paths_policy.md, docs/human/ProjectStructure.md.

## 6) Ключевые модули и фичи
- composeApp (UI/Presentation, навигация):
  - features/chat, /metrics, /mcp, /reminder, /settings — экраны, ViewModel, MVI.
- shared (Domain/Data/DI):
  - domain: контракты репозиториев, доменные модели, use case.
  - data: OpenAIApi/Impl, репозитории (LLMClientRepositoryImpl, DialogPersistenceRepositoryImpl, ProviderSettingsRepositoryImpl, ReminderRepositoryImpl), мапперы, БД и DataStore.
  - di: SharedModule + вспомогательные модули (Parsers/Metrics/Mcp/Compression/Platform/DataStoreProvider).
- server (Ktor):
  - module(): WebSockets, маршруты, обработка JSON-RPC, инструменты MCP.

## 7) Основные пользовательские сценарии (critical paths)
1) Чат:
   - ChatEvent.Send(text) → UseCase → LLMClientRepository → OpenAIApiImpl (Ktor) → DTO→Domain → ChatState обновляется → UI рендерит.
2) MCP-инструменты:
   - UI запрашивает список инструментов → WS /mcp → tools/list.
   - Вызов инструмента → tools/call → результат в UI.
3) Настройки:
   - Обновление конфигурации провайдера/LLM → DataStore через ProviderSettingsRepository/LLMConfigRepository.
4) Напоминания:
   - Создание/планирование → ReminderRepository → ExecuteReminderTaskUseCase → ReminderEngine (notifier по умолчанию Noop).
5) Метрики:
   - Просмотр собранных метрик/логов; диагностика состояния чата/запросов.

## 8) Технический контекст и окружение
- Клиентские таргеты: Android, Desktop (JVM).
- Сервер: Netty (Ktor), порт SERVER_PORT (см. shared/common/Constants.kt).
- Сборка/запуск (из README.md):
  - Android: ./gradlew :composeApp:assembleDebug
  - Desktop: ./gradlew :composeApp:run
  - Server: ./gradlew :server:run
- MCP:
  - WS эндпоинт: ws://localhost:8080/mcp
  - Поддерживаемые инструменты: health_check, get_time, get_rub_usd_rate, echo, sum, github.list_user_repos, github.list_my_repos (требует GITHUB_TOKEN).

## 9) Зависимости и интеграции
- Koin — DI модульность.
- Ktor — HTTP/WebSocket.
- Kotlinx.serialization — сериализация JSON.
- DataStore — персистентность конфигов.
- БД — AppDatabase/DatabaseFactory (реализация под платформы в androidMain/jvmMain).
- GitHub API — часть MCP-инструментов на сервере.
- OpenAI API — через OpenAIApi/Impl (ключи/секреты должны маскироваться в логах).

## 10) Ограничения и допущения
- DTO строго изолированы в Data; Presentation/Domain DTO не видят.
- Ошибки приводятся к DomainException на границе Data→Domain.
- Максимум кода — в commonMain; платформенные провайдеры подменяются через DI.
- Навигация не хардкодится во ViewModel; только через UI-колбэки.

## 11) Критерии успеха (метрики)
- Стабильная отправка/получение сообщений в чате (низкая доля сетевых ошибок).
- Успешные вызовы MCP-инструментов с предсказуемыми ошибками при неправильных входных данных.
- Конфиги провайдера/LLM сохраняются между сессиями.
- Логи и метрики покрывают основные пути и помогают локализовать сбои.
- Проект собирается и запускается на Android и Desktop без правок кода.

## 12) Риски
- Сетевые ограничения/квоты внешних API (OpenAI/GitHub).
- Платформенные различия KMP (особенности DataStore/БД).
- Эволюция протокола MCP (совместимость при обновлениях).

## 13) Связанные документы
- docs/INDEX.md — навигация по документации.
- docs/human/* — архитектура, структура проекта, код-стайл, флоу фич.
- docs/llm/* — правила для LLM и политика путей.
- docs/inventory/project_inventory.json — инвентаризация ключевых файлов.

## 14) Связь с банком памяти
- Этот документ (projectbrief.md) — источник истины по объёму и целям.
- productContext.md — детали продуктового контекста (почему/для кого/ценность).
- systemPatterns.md — архитектурные решения, слои, отношения компонентов.
- techContext.md — технологический стек, окружение разработки, зависимости/версии.
- activeContext.md — текущий фокус/следующие шаги/активные решения.
- progress.md — статус выполнения, известные проблемы, эволюция решений.

## 15) Следующие шаги по банку памяти
- Создать/актуализировать productContext.md, systemPatterns.md, techContext.md на базе этого брифа.
- В activeContext.md зафиксировать текущее состояние и ближайшие шаги по фичам (chat/MCP/reminder/settings/metrics).
- В progress.md отслеживать готовность сценариев и проблемы.

— Конец projectbrief.md —
