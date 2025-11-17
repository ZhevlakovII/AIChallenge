# Руководство по разработке фич (Feature Blueprint)

Документ описывает стандартный процесс добавления новой фичи в проект AIChallenge. Он согласован с AIChallenge-StyleGuide.md, .clinerules/Project-rules.md, архитектурой (чистая архитектура, MVI, KMP, Koin, Ktor, Coroutines) и структурой проекта.

Содержание:
- Цели и принципы
- Структура фичи
- Шаги разработки (Domain → Data → DI → Presentation)
- Навигация (Compose)
- Правила маппинга DTO ↔ Domain
- Обработка ошибок и логирование
- Тестирование и DoD (Definition of Done)
- Чек-листы

## Цели и принципы

- Модульность по фичам: `features/<feature>/{di,domain,presentation,data}`.
- Чистая архитектура: UI/Presentation зависит только от Domain-контрактов; реализации — в Data.
- MVI: однонаправленный поток (Event → VM → UseCase → Repo → New State).
- KMP: максимум общего кода в `commonMain`, платформенные реализации — в `androidMain`/`jvmMain` за интерфейсами.
- Koin: DI на уровне интерфейсов; фабрики/синглтоны инициализируются в модуле фичи.

## Структура фичи

Рекомендуемый шаблон (для `composeApp/src/commonMain/kotlin/ru/izhxx/aichallenge/features/<feature>`):

```
<feature>/
  ├── di/
  │   └── <Feature>Module.kt
  ├── domain/
  │   ├── model/                 // доменные модели (опционально)
  │   └── usecase/               // интерфейсы и реализации UseCase
  ├── presentation/
  │   ├── <Feature>Screen.kt
  │   ├── <Feature>ViewModel.kt
  │   ├── model/
  │   │   ├── <Feature>State.kt
  │   │   └── <Feature>Event.kt
  │   └── components/            // UI-компоненты
  └── data/                      // при необходимости: репозитории/источники/мапперы/DTO
```

Если логика данных/моделей общая для нескольких фич — переносить в `shared/` (Domain/Data слои).

## Шаги разработки

### 1) Domain (контракты и use cases)

- Опишите доменные модели (чистые классы/enum, без платформенных зависимостей).
- Задайте контракты репозиториев в `shared/src/commonMain/.../domain/repository`.
- Создайте UseCase интерфейсы и их реализации в фиче (или в `shared`, если используются несколькими фичами).

Пример:
```kotlin
// features/sample/domain/usecase/FetchItemsUseCase.kt
interface FetchItemsUseCase {
    suspend operator fun invoke(): List<Item>
}

// features/sample/domain/usecase/FetchItemsUseCaseImpl.kt
class FetchItemsUseCaseImpl(
    private val repository: SampleRepository
) : FetchItemsUseCase {
    override suspend fun invoke(): List<Item> = repository.getItems()
}
```

### 2) Data (реализации, источники, мапперы)

- Реализации репозиториев располагаются в `data/` фичи либо в `shared/data/repository` (если переиспользуются).
- DTO-модели (суффикс DTO) — в `data/model`.
- Мапперы DTO ↔ Domain — рядом с реализацией (extension-функции предпочтительны).
- Вызовы сети/БД — через Ktor/DAO/DataStore; ошибки нормализуются (`safeApiCall`).

Пример:
```kotlin
// features/sample/data/SampleRepositoryImpl.kt
class SampleRepositoryImpl(
    private val api: SampleApi
) : SampleRepository {

    override suspend fun getItems(): List<Item> = safeApiCall {
        api.fetchItems().items.map { it.toDomain() }
    }
}

// features/sample/data/model/ItemDTO.kt
@Serializable
data class ItemDTO(val id: String, val title: String)

fun ItemDTO.toDomain() = Item(id = id, title = title)
```

### 3) DI (Koin)

- Создайте модуль DI фичи в `features/<feature>/di/<Feature>Module.kt`.
- Регистрируйте: UseCase-ы, реализации репозиториев/источников, ViewModel.

Пример:
```kotlin
// features/sample/di/SampleModule.kt
val sampleModule = module {
    factory<FetchItemsUseCase> { FetchItemsUseCaseImpl(get()) }
    factory<SampleRepository> { SampleRepositoryImpl(get()) }
    viewModel { SampleViewModel(fetchItems = get()) }
}
```

Подключение модуля фичи — в общем App/DI (например, `composeApp/src/commonMain/.../di/AppModule.kt` включает модули фич).

### 4) Presentation (MVI, Compose)

- Опишите MVI-контракт: `State` и `Event` (sealed class), при необходимости `Effect`.
- ViewModel хранит `MutableStateFlow<State>`, наружу экспонирует `StateFlow<State>`.
- UI (Compose) подписывается на состояние и отправляет события.

Пример:
```kotlin
// features/sample/presentation/model/SampleState.kt
data class SampleState(
    val isLoading: Boolean = false,
    val items: List<Item> = emptyList(),
    val error: String? = null
)

// features/sample/presentation/model/SampleEvent.kt
sealed class SampleEvent {
    object Refresh : SampleEvent()
    data class Select(val id: String) : SampleEvent()
}

// features/sample/presentation/SampleViewModel.kt
class SampleViewModel(
    private val fetchItems: FetchItemsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(SampleState(isLoading = true))
    val state: StateFlow<SampleState> = _state.asStateFlow()

    fun processEvent(event: SampleEvent) {
        when (event) {
            is SampleEvent.Refresh -> refresh()
            is SampleEvent.Select -> onSelect(event.id)
        }
    }

    private fun refresh() = viewModelScope.launch {
        _state.update { it.copy(isLoading = true, error = null) }
        runCatching { fetchItems() }
            .onSuccess { items -> _state.update { it.copy(isLoading = false, items = items) } }
            .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
    }

    private fun onSelect(id: String) {
        // навигация/обработка выбора
    }
}
```

UI:
```kotlin
@Composable
fun SampleScreen(
    viewModel: SampleViewModel = koinViewModel(),
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // ... отрисовка состояния, отправка событий
}
```

## Навигация (Compose)

- Маршруты объявляются в `App.kt` (sealed class Screen + NavHost).
- Для передачи аргументов используются параметры маршрута/`navArgument`.
- Переходы инициируются из UI через колбэки/события VM.

Пример добавления маршрута:
```kotlin
sealed class Screen(val route: String) {
    object Chat : Screen("chat")
    object Sample : Screen("sample")
}

NavHost(navController, startDestination = Screen.Chat.route) {
    composable(Screen.Sample.route) { SampleScreen(onBack = { navController.popBackStack() }) }
}
```

## Правила маппинга DTO ↔ Domain

- DTO-модели не выходят за пределы Data-слоя.
- Мапперы оформляются как extension-функции:
  - `fun SomeDTO.toDomain(): SomeDomain`
  - `fun SomeDomain.toDto(): SomeDTO` (если требуется двусторонний маппинг)
- Валидация и нормализация данных выполняются на границе слоёв.
- Nullability/дефолты — задаются в мапперах, доменные модели максимально безопасны.

## Обработка ошибок и логирование

- Все внешние вызовы оборачиваются в `safeApiCall { ... }`.
- Низкоуровневые ошибки (RequestError/ApiError) конвертируются в `DomainException`.
- ViewModel и UI работают с человеко-ориентированными сообщениями (без технических деталей).
- Логирование:
  - Использовать общий `Logger` из shared.
  - Уровни: debug/info/error; маскировать секреты.

## Тестирование и Definition of Done (DoD)

Юнит-тесты:
- Domain: тесты UseCase-ов (чистые, без платформенных зависимостей).
- Data: тестирование мапперов, обработчиков ошибок, репозиториев (через doubles/fakes).
- Presentation: тестирование редьюсеров/логики VM; Compose snapshot-тесты для UI при необходимости.

Интеграционные тесты (опционально):
- Простой happy-path e2e через репозитории и use cases с тестовыми зависимостями.

DoD чек-лист:
- [ ] Структура фичи соответствует шаблону.
- [ ] MVI-контракт оформлен (State/Event/(Effect)).
- [ ] Domain: контракты, UseCase-ы реализованы и покрыты тестами.
- [ ] Data: реализации репозиториев, мапперы, `safeApiCall`, ошибки нормализованы.
- [ ] DI: модуль фичи создан и подключён.
- [ ] UI: экран и компоненты, состояние обновляется через StateFlow.
- [ ] Навигация: маршрут добавлен, параметры прокидываются корректно.
- [ ] KDoc на публичных классах/методах (RU).
- [ ] Логирование ключевых операций.
- [ ] Тесты выполняются, линтер чист, сборка зелёная.

## Краткий чек-лист создания фичи

1) Сформулируйте доменные требования и данные.
2) Опишите контракты репозиториев и UseCase-ы (Domain).
3) Реализуйте слой Data (источники, мапперы, обработка ошибок).
4) Настройте DI-модуль фичи (Koin).
5) Реализуйте Presentation: State, Event, ViewModel, Screen.
6) Добавьте маршрут в навигацию (App.kt).
7) Напишите тесты (Domain/Data/UI).
8) Обновите документацию при необходимости (docs/human, docs/llm).
9) Проверьте DoD и закройте задачу.
