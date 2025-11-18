# AIChallenge — правила генерации кода (LLM)

Документ определяет строгие правила, по которым LLM вносит изменения в код и документацию проекта AIChallenge. Согласован с AIChallenge-StyleGuide.md, .clinerules/Project-rules.md, а также с архитектурой (Чистая архитектура, MVI, KMP, Koin, Ktor, Coroutines, Kotlinx.serialization).

Содержание:
- Общие принципы и ограничения
- Слои и правила зависимостей
- Правила для KMP и размещения файлов
- UI и MVI (State/Event/Effect, ViewModel, StateFlow)
- Data слой: API, DTO, маппинг, safeApiCall, ошибки
- DI (Koin) и навигация
- Коррутины и диспетчеры
- Логирование и метрики
- Тестирование
- Формат предложений изменений и патчи

## Общие принципы и ограничения

- Запрещено нарушать границы слоёв:
  - Presentation видит только Domain-контракты; Data-реализации скрыты за интерфейсами.
  - DTO не могут проникать в Presentation/Domain. Преобразование DTO↔Domain — в Data.
- Соблюдать модульность по фичам: `features/<feature>/{di,domain,presentation,data}` в composeApp (общий UI-код).
- Соблюдать KDoc на русском для всех публичных API/классов/методов. Документировать нетривиальные участки.
- Именование:
  - Классы: UpperCamelCase (ChatViewModel)
  - Интерфейсы: без префикса `I`
  - Реализации: суффикс `Impl`
  - DTO: суффикс `DTO`
  - UseCase: Глагол+Сущ. (SendMessageUseCase)
  - Константы: UPPER_SNAKE_CASE
- Все изменения должны укладываться в текущую структуру проекта и следовать практикам Чистой архитектуры, MVI, Koin, Ktor, Coroutines.

## Слои и правила зависимостей

- Presentation (composeApp/src/commonMain/.../features/.../presentation):
  - UI-компоненты Compose, ViewModel, модели состояния/событий.
  - Только доменные модели/контракты (никаких DTO).
- Domain (shared/src/commonMain/.../domain):
  - Чистые модели и интерфейсы репозиториев, UseCase-ы.
  - Без платформенной специфики.
- Data (shared/src/commonMain/.../data):
  - Реализации репозиториев, источники данных, DTO-модели, мапперы DTO↔Domain.
  - Сетевая логика (Ktor), сериализация (Kotlinx.serialization), БД/кэш/DataStore.
- DIP:
  - Domain определяет интерфейсы.
  - Data реализует интерфейсы и регистрируется в DI.
  - Presentation зависит от интерфейсов Domain и UseCase-ов.

## Правила для KMP и размещения файлов

- Общий код максимально в `commonMain`.
- Платформенная специфика — в `androidMain`/`jvmMain` и инжектируется через интерфейсы (DI).
- Размещение:
  - UI (Compose): `composeApp/src/commonMain/kotlin/ru/izhxx/aichallenge/features/...`
  - Shared Domain/Data/DI: `shared/src/commonMain/kotlin/ru/izhxx/aichallenge/...`
  - Платформенные адаптеры: `shared/src/androidMain/...`, `shared/src/jvmMain/...`
  - Сервер (Ktor): `server/src/main/kotlin/...`

## UI и MVI

- Контракт MVI:
  - State: data class, только неизменяемые поля.
  - Event: sealed class.
  - Effect: при необходимости отдельный одноразовый поток (SharedFlow).
- ViewModel:
  - Хранит `MutableStateFlow<State>`, наружу отдаёт `StateFlow<State>`.
  - Обрабатывает события в функции `processEvent(event: XEvent)`.
  - Асинхронная логика — через `viewModelScope.launch { ... }`.
- Compose:
  - Подписка на состояние: `collectAsStateWithLifecycle()`.
  - UI чисто отображает `State`, события отправляет в `ViewModel`.
- Пример сигнатур:
  - `data class FeatureState(...)`
  - `sealed class FeatureEvent { ... }`
  - `class FeatureViewModel(...) : ViewModel() { val state: StateFlow<FeatureState> ... }`

## Data слой: API, DTO, маппинг, safeApiCall, ошибки

- API: Ktor + Kotlinx.serialization. DTO лежат в `shared/.../data/model` (суффикс DTO).
- Маппинг:
  - Extension-функции: `fun SomeDTO.toDomain(): SomeDomain`.
  - Никаких DTO в Presentation/Domain.
- Вызовы к внешним системам/БД оборачивать в `safeApiCall { ... }` (см. `shared/common/SafeApiCall.kt`).
- Ошибки:
  - Низкоуровневые: `RequestError`, `ApiError` (data/error).
  - Доменные: `DomainException` (domain/model/error).
  - Data-слой конвертирует ошибки в `DomainException` с понятным message.

## DI (Koin) и навигация

- DI:
  - Для каждой фичи — модуль в `features/<feature>/di/<Feature>Module.kt`.
  - Регистрировать UseCase-ы, реализации репозиториев/источников, ViewModel.
  - Общие модули — в `shared/src/commonMain/.../di`.
- Навигация:
  - Маршруты и аргументы — в `composeApp/src/commonMain/.../App.kt`.
  - Не хардкодить навигацию внутри ViewModel; использовать колбэки/внешние навигационные функции.

## Коррутины и диспетчеры

- Использовать structured concurrency.
- Явно указывать диспетчеры при тяжёлых операциях (например, `withContext(Dispatchers.IO)`), при этом диспетчеры предпочтительно инжектировать через DI/абстракции, если используется вне ViewModel.
- В ViewModel использовать `viewModelScope`.
- Не блокировать главный поток; долгие операции — вне Main.

## Логирование и метрики

- Логер: общий `Logger` (`shared/common/Logger.kt`).
- Уровни: debug/info/error, структурируйте контекст.
- Маскировать секреты в логах.
- Ключевые бизнес-события и ошибки — логировать.

## Тестирование

- Размещать тесты:
  - `composeApp/src/commonTest/kotlin` — для UI/ViewModel.
  - `shared/src/commonTest/kotlin` — для Domain/Data (мапперы, use case, репозитории).
- Unit-тесты Domain/Data без платформенной специфики.
- Для UI — snapshot/compose-testing (при необходимости).

## Формат предложений изменений и патчи

- Перед внесением изменений:
  - Краткий план: список файлов, краткое описание изменений, последствия.
- Для правок в существующих файлах использовать точечные диффы:
  - Применять инструмент замены с SEARCH/REPLACE блоками и точным совпадением строк.
  - Избегать переписывания больших файлов, если правка локальная.
- Для новых файлов — полное содержание файла в одном блоке.
- Порядок действий:
  1) Сформировать план (перечень файлов/правок).
  2) Применить изменения по шагам.
  3) Проверить сборку/линты/тесты (если применимо).
- Требования к результату:
  - Валидная компиляция (если код менялся).
  - Соответствие правилам слоёв и KMP-размещения.
  - KDoc на публичных API.
  - Соответствие StyleGuide и данному документу.

---

Кратко: генерируй MVI-совместимый UI на Compose, чистый Domain, Data с Ktor и DTO-изоляцией, DI через Koin, асинхронность на Coroutines с `safeApiCall`, строгие границы слоёв и KMP, логирование/метрики, тесты и точечные патчи.
