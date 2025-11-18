# Обработка ошибок

Документ описывает общую стратегию обработки ошибок в AIChallenge на всех слоях (Data/Domain/Presentation), согласованную с Чистой архитектурой, KMP, MVI, Koin и сетевым стеком Ktor. Основан на AIChallenge-StyleGuide.md и .clinerules/Project-rules.md.

Содержание:
- Цели и принципы
- Иерархия ошибок и ответственность слоёв
- safeApiCall: единая точка нормализации
- Сетевые и парсинговые ошибки (Data)
- Доменные ошибки (Domain)
- Отображение ошибок в UI (Presentation)
- Логирование и телеметрия ошибок
- Примеры и шаблоны
- Чек-лист качества

## Цели и принципы

- Единообразная, предсказуемая обработка ошибок, не зависящая от платформы.
- Разделение ответственности:
  - Data: захват и нормализация низкоуровневых ошибок; преобразование к доменной форме.
  - Domain: работа только с доменными ошибками (без привязки к инфраструктуре).
  - Presentation: пользовательски-ориентированные сообщения, без технических деталей.
- Исключение «протекания» деталей сетевого стека и DTO в Presentation/Domain.

## Иерархия ошибок и ответственность слоёв

- Data (низкий уровень):
  - Низкоуровневые ошибки сети, сериализации, I/O фиксируются и маппятся.
  - Файлы:
    - shared/src/commonMain/kotlin/ru/izhxx/aichallenge/data/error/ApiError.kt
    - shared/src/commonMain/kotlin/ru/izhxx/aichallenge/data/error/RequestError.kt

- Domain (средний уровень):
  - Унифицированная доменная ошибка: `DomainException`
  - Файл:
    - shared/src/commonMain/kotlin/ru/izhxx/aichallenge/domain/model/error/DomainException.kt

- Presentation (высокий уровень):
  - UI-сообщения на основе информации из DomainException.
  - Дополнительные элементы UX (баннеры, snackbar, retry‑кнопки).

## safeApiCall: единая точка нормализации

- Общая функция:
  - shared/src/commonMain/kotlin/ru/izhxx/aichallenge/common/SafeApiCall.kt
- Назначение:
  - Оборачивать внешние вызовы (Ktor, БД/DAO, DataStore).
  - Перехватывать исключения (сетевые, сериализация, время ожидания).
  - Конвертировать к доменной ошибке `DomainException` с понятным текстом.
- Правило:
  - Все потенциально «опасные» вызовы в Data-слое должны использовать `safeApiCall { ... }`.

## Сетевые и парсинговые ошибки (Data)

- Источники:
  - Ktor-клиент (таймауты, недоступность сети, ошибки протокола).
  - kotlinx.serialization (парсинг/формат ответа).
- Модель:
  - RequestError — ошибки транспорта/запроса.
  - ApiError — бизнес-ошибки API (коды/сообщения) при валидном ответе.
- Политики:
  - Таймауты/повторы (retry/backoff) задаются на уровне конфигурации Ktor-клиента (через DI).
  - Логирование запросов/ответов с маскировкой секретов.
  - Конверсия: любые низкоуровневые исключения — в `DomainException` с user-friendly message.

## Доменные ошибки (Domain)

- Унифицированная доменная ошибка:
  - `DomainException(message: String, cause: Throwable? = null, isError: Boolean = true)`
- Правила:
  - Только DomainException «поднимается» выше, в Presentation.
  - Допускается использование `isError = false` для доброжелательных уведомлений (успешных операций), если это принято в конкретной фиче (например, snackbar об успешном удалении).

## Отображение ошибок в UI (Presentation)

- MVI:
  - Ошибка — часть UI-состояния (например, `error: DomainException?`).
  - Одноразовые уведомления — отдельный эффект/канал или «сброс» ошибки после показа.
- UX-рекомендации:
  - Краткие и понятные сообщения для пользователя.
  - Предлагать действия: «Повторить», «Настройки», «Открыть сеть».
  - Не показывать технические детали (стэктрейсы, коды HTTP) пользователю.

Пример вью-модели (подход из планируемого экрана истории):
- После неудачной операции — поместить `DomainException` в состояние.
- Показать Snackbar; через задержку/действие пользователя очистить ошибку: `_state.update { it.copy(error = null) }`.

## Логирование и телеметрия ошибок

- Логгер:
  - shared/src/commonMain/kotlin/ru/izhxx/aichallenge/common/Logger.kt
- Правила:
  - Логировать на Data-слое исходные детали (уровень debug/error).
  - Маскировать секреты и персональные данные.
  - На Presentation-слое логировать ключевые бизнес-события (например, неуспешная отправка сообщения).
- Метрики:
  - Фиксировать частоту ошибок, время ответа, коды, точки отказа.
  - Экран метрик: composeApp/src/commonMain/kotlin/ru/izhxx/aichallenge/features/metrics/ChatMetricsScreen.kt

## Примеры и шаблоны

Пример использования safeApiCall в репозитории:
```kotlin
suspend fun loadSomething(): DomainModel = safeApiCall {
    val dto = api.getSomething()
    dto.toDomain()
}
```

Пример обработки ошибки в VM:
```kotlin
viewModelScope.launch {
    _state.update { it.copy(isLoading = true, error = null) }
    runCatching { repository.loadSomething() }
        .onSuccess { data -> _state.update { it.copy(isLoading = false, data = data) } }
        .onFailure { e ->
            val error = e as? DomainException ?: DomainException("Не удалось загрузить данные", e)
            _state.update { it.copy(isLoading = false, error = error) }
        }
}
```

Пример одноразового уведомления (snackbar) и сброса:
```kotlin
_state.update { it.copy(error = DomainException("Операция выполнена", isError = false)) }
viewModelScope.launch {
    kotlinx.coroutines.delay(2000)
    _state.update { it.copy(error = null) }
}
```

## Чек-лист качества

- [ ] Все внешние вызовы в Data-слое обёрнуты в `safeApiCall`.
- [ ] Конверсия низкоуровневых исключений в `DomainException` реализована.
- [ ] DTO/транспортные детали не протекают в Domain/Presentation.
- [ ] UI показывает понятные сообщения и даёт действие «Повторить».
- [ ] Ошибки логируются на корректном уровне; секреты маскируются.
- [ ] Для частых ошибок предусмотрены ретраи/таймауты и тесты.
- [ ] Тесты проверяют сценарии ошибок (сеть, парсинг, оффлайн).
