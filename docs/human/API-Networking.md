# API и сетевое взаимодействие (Ktor)

Документ описывает стандарты настройки и использования сетевого стека в AIChallenge. Основывается на Ktor-клиенте, kotlinx.serialization, принципах Чистой архитектуры, KMP и MVI. Согласован с AIChallenge-StyleGuide.md и .clinerules/Project-rules.md.

Содержание:
- Цели и требования
- Конфигурация Ktor-клиента
- Сериализация (kotlinx.serialization)
- Таймауты, retry/backoff
- Логирование и маскирование секретов
- Политика ошибок и safeApiCall
- Архитектурные правила (слои, DTO, мапперы)
- Примеры
- Чек-лист качества

## Цели и требования

- Единая конфигурация Ktor-клиента через DI (Koin).
- Стабильность: таймауты, повторы, обработка ошибок, маскирование секретов.
- Изоляция DTO в Data-слое, маппинг DTO ↔ Domain.
- KMP: переносимость конфигурации, платформенные детали — в соответствующих source sets.

## Конфигурация Ktor-клиента

- Клиент создаётся и конфигурируется в DI-модулях.
- Включаем плагины:
  - ContentNegotiation (kotlinx.serialization)
  - HttpTimeout (connect/read/write)
  - DefaultRequest (базовый URL, заголовки)
  - Logging (с собственной стратегией маскирования)
  - (опц.) HttpRequestRetry — либо собственная реализация retry/backoff

Рекомендации:
- Базовый URL и ключи — через конфигурацию и безопасные хранилища (DataStore/Env/Secrets).
- Явно указывать User-Agent.
- Настроить пулы соединений и ограничения (по необходимости).

## Сериализация (kotlinx.serialization)

- DTO в `shared/src/commonMain/kotlin/ru/izhxx/aichallenge/data/model/*` (суффикс DTO).
- Json-конфиг:
  - ignoreUnknownKeys = true
  - isLenient = true (по необходимости)
  - explicitNulls = false (если приемлемо)
- Любые нестандартные поля/адаптеры — отдельные сериалайзеры.

## Таймауты, retry/backoff

- HttpTimeout:
  - connectTimeoutMillis, requestTimeoutMillis, socketTimeoutMillis — значения подбирать под продуктовые требования, хранить в конфигурации.
- Retry/backoff:
  - Повторы для идемпотентных запросов (GET), экспоненциальная задержка.
  - Для POST/PUT — осторожно, только если серверно допустимо.
  - Прерывание после N попыток, логирование каждого retry.

## Логирование и маскирование секретов

- Логирование запросов/ответов включать на debug с маскированием:
  - Authorization, api-key, токены — заменять на "***".
  - Тела запросов/ответов логировать только в безопасных окружениях и при явном флаге.
- Корреляция:
  - Присваивать корреляционный ID запросу (header), логировать его для трассировки.

## Политика ошибок и safeApiCall

- Все внешние вызовы в репозиториях — через `safeApiCall { ... }`:
  - Перехватывает исключения Ktor/IO/serialization.
  - Нормализует к `DomainException` с user-friendly сообщением.
- Разделение:
  - RequestError/ApiError — в Data-слое.
  - DomainException — единственная форма ошибок выше по слоям.

Ссылки:
- shared/src/commonMain/kotlin/ru/izhxx/aichallenge/common/SafeApiCall.kt
- shared/src/commonMain/kotlin/ru/izhxx/aichallenge/data/error/*

## Архитектурные правила

- DTO остаются в Data, маппинг в доменные модели — до выхода из Data (extension-функции).
- Репозитории инкапсулируют клиент Ktor и мапперы; наружу отдают доменные модели.
- Конфигурация клиента — через DI, не создавать клиенты в местах использования.

## Примеры

Базовая конфигурация клиента (псевдокод):
```kotlin
fun provideHttpClient(json: Json): HttpClient = HttpClient {
    install(ContentNegotiation) { json(json) }
    install(HttpTimeout) {
        connectTimeoutMillis = 10_000
        socketTimeoutMillis = 30_000
        requestTimeoutMillis = 30_000
    }
    install(DefaultRequest) {
        header(HttpHeaders.UserAgent, "AIChallenge/1.0")
        // базовый URL можно задавать на уровне конкретных запросов/эндпоинтов
    }
    install(Logging) {
        logger = object : Logger {
            override fun log(message: String) {
                // Маскировать секреты и при необходимости ограничивать длину
            }
        }
        level = LogLevel.HEADERS // или BODY в dev-окружениях
        sanitizeHeader { header -> header.equals("Authorization", true) || header.contains("api-key", true) }
    }
    // (опц.) политика повторов
}
```

Вызов API в репозитории:
```kotlin
suspend fun loadData(): DomainModel = safeApiCall {
    val dto: SomeDTO = httpClient.get("$baseUrl/path").body()
    dto.toDomain()
}
```

Маппер DTO → Domain:
```kotlin
fun SomeDTO.toDomain(): SomeDomain =
    SomeDomain(id = id, title = title ?: "—", updatedAt = updatedAt ?: 0L)
```

## Чек-лист качества

- [ ] Конфигурация клиента вынесена в DI, переиспользуема.
- [ ] Включены ContentNegotiation/HttpTimeout/Logging; маскирование секретов работает.
- [ ] Настроены разумные таймауты; retry/backoff для идемпотентных запросов.
- [ ] Все вызовы обёрнуты в `safeApiCall`, ошибки нормализуются.
- [ ] DTO не выходят за пределы Data, маппинг оформлен extension-функциями.
- [ ] Логи не содержат секретов, есть корреляционные идентификаторы.
