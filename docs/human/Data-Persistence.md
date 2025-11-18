# Данные и персистентность

Документ описывает подход к хранению данных в AIChallenge: базы данных/DAO/Entity, DataStore/конфиги, кэширование и оффлайн-поведение. Согласован с Чистой архитектурой, KMP, MVI, Koin, Ktor и правилами из AIChallenge-StyleGuide.md и .clinerules/Project-rules.md.

Содержание:
- Цели и принципы
- Архитектурные уровни и ответственность
- База данных и DAO (общие положения)
- DataStore и конфигурации (ключи/настройки)
- Кэширование и оффлайн-режим
- Моделирование данных: Entity/DTO/Domain
- Ошибки и транзакционная целостность
- Логирование и метрики доступа к данным
- Примеры
- Чек-лист качества

## Цели и принципы

- Единый подход к персистентности, переносимый в KMP-контексте.
- Разделение моделей: Entity (БД), DTO (транспорт), Domain (бизнес).
- Инкапсуляция доступа к данным в репозиториях, DAO не «просачиваются» в Domain/Presentation.
- Безопасная работа с данными (валидация, миграции, обработка ошибок).
- Предсказуемый оффлайн-режим (по возможности) и кэширование часто используемых данных.

## Архитектурные уровни и ответственность

- Data слой:
  - DAO/Entity/Database, DataStore, кэш.
  - Репозитории объединяют источники (локальные/удалённые), выполняют маппинг Entity↔Domain/DTO↔Domain.
- Domain слой:
  - Оперирует только доменными моделями и контрактами репозиториев, без знаний о DAO/Entity.
- Presentation слой:
  - Получает доменные модели и управляет состоянием UI; отсутствует доступ к DAO/Entity/DTO.

## База данных и DAO

- Расположение (общие части):
  - `shared/src/commonMain/kotlin/ru/izhxx/aichallenge/data/database/**`
  - DAO: `data/database/dao/*` — `ChatHistoryDao.kt`, `DialogDao.kt`, `MessageDao.kt`, `SummaryDao.kt`
  - Entity: `data/database/entity/*` — `ChatHistoryEntity.kt`, `DialogEntity.kt`, `MessageEntity.kt`, `SummaryEntity.kt`
  - Фабрики/инициализация: `data/database/DatabaseFactory.kt`, `AppDatabase.kt`
- Правила:
  - Entity — только для локального хранилища; не использовать в Domain/Presentation.
  - DAO — интерфейсы для CRUD-операций; вызываются из репозиториев.
  - Транзакции (если поддерживаются) — оформлять в Data-слое.
  - Миграции — документировать и тестировать отдельно (при появлении).

## DataStore и конфигурации

- Расположение:
  - Общий контракт/provider: `shared/src/commonMain/kotlin/ru/izhxx/aichallenge/di/DataStoreProvider.kt`
  - Платформенные реализации:
    - Android: `shared/src/androidMain/.../di/DataStoreProvider.android.kt`
    - JVM: `shared/src/jvmMain/.../di/DataStoreProvider.jvm.kt`
- Использование:
  - Хранение конфигов (например, API-ключи, настройки провайдера, флаги).
  - Доступ к ключам только в Data/DI; Presentation не оперирует секретами напрямую.
- Безопасность:
  - Маскировать секреты в логах, не сериализовать приватные данные в UI-стороны.

## Кэширование и оффлайн-режим

- Цели:
  - Минимизировать сетевые запросы, обеспечить базовую функциональность без сети.
- Подход:
  - Прежде чем обратиться к сети — использовать кэш (если данные присутствуют и не протухли).
  - При успешном сетевом ответе — обновлять локальное хранилище.
  - При ошибках сети — отдавать данные из кэша (если доступны), уведомляя UI о деградации.
- Политики:
  - TTL и правила инвалидации определяются по сущностям (конфигурируемо).
  - Метрики попаданий/промахов кэша фиксируются (см. Logging-Metrics).

## Моделирование данных: Entity / DTO / Domain

- Entity:
  - Отражает схему БД; без бизнес-логики.
  - Не выходит за границы Data-слоя.
- DTO (суффикс DTO):
  - Транспортные модели для сетевого слоя (см. `data/model/*`).
  - Не выходят за границы Data; маппятся в Domain.
- Domain:
  - Чистые модели для бизнес-логики/презентации.
  - Не зависят от Ktor/DB/платформенных типов.

Маппинг:
- Entity ↔ Domain: для локального хранения доменных сущностей.
- DTO ↔ Domain: для сетевого обмена.
- Реализуется extension-функциями рядом с реализациями (Data-слой).

## Ошибки и транзакционная целостность

- Все IO-операции оборачивать в `safeApiCall { ... }` (или аналог, если операция локальная).
- Низкоуровневые исключения (IO/SQL/парсинг) конвертируются в `DomainException` с user-friendly сообщением.
- При транзакциях: атомарность изменений, откат при частичных сбоях.
- Логировать ошибки с контекстом (идентификаторы сущностей/операций), без раскрытия секретов.

## Логирование и метрики доступа к данным

- Логирование:
  - Уровни debug/info/error; контекст операций (тип сущности, размер выборки, ids).
  - Секреты/чувствительные данные — маскировать.
- Метрики:
  - Частота операций чтения/записи/удаления.
  - TTL-контроль, промахи/попадания кэша.
  - Длительность операций и частота ошибок.

## Примеры

Сохранение результатов сети в локальное хранилище:
```kotlin
suspend fun syncMessages(dialogId: String): List<Message> = safeApiCall {
    val response = api.getMessages(dialogId) // DTO
    val domain = response.items.map { it.toDomain() }
    dao.replaceMessages(dialogId, domain.map { it.toEntity(dialogId) })
    domain
}
```

Маппинг Entity ↔ Domain:
```kotlin
data class MessageEntity(val id: String, val dialogId: String, val content: String, val createdAt: Long)

fun MessageEntity.toDomain(): Message =
    Message(id = id, content = content, createdAt = createdAt)

fun Message.toEntity(dialogId: String): MessageEntity =
    MessageEntity(id = id, dialogId = dialogId, content = content, createdAt = createdAt)
```

DataStore (псевдокод):
```kotlin
interface DataStoreProvider {
    suspend fun saveApiKey(provider: String, key: String)
    suspend fun readApiKey(provider: String): String?
}
```

## Чек-лист качества

- [ ] DAO/Entity используются только в Data-слое; нет утечек в Domain/Presentation.
- [ ] DTO не выходят за Data; мапперы оформлены extension-функциями.
- [ ] Репозитории инкапсулируют локальные и удалённые источники, содержат логику синхронизации.
- [ ] Оффлайн-режим реализован (если релевантно); есть политика TTL/инвалидации.
- [ ] IO-операции обёрнуты в `safeApiCall`, ошибки конвертируются в `DomainException`.
- [ ] Логи не раскрывают секреты; метрики телеметрии собираются по ключевым операциям.
