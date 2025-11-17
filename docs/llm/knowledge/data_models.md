# Данные и модели (сводка для LLM)

Короткая сводка по моделям данных и правилам маппинга в AIChallenge. Подробно см.: 
- docs/human/Data-Persistence.md
- docs/human/API-Networking.md
- docs/human/ErrorHandling.md
- docs/llm/file_paths_policy.md
- docs/llm/code_generation_rules.md

Полный список файлов см. инвентарь: docs/inventory/project_inventory.json.

Содержание:
- Слои моделей: DTO / Entity / Domain / UI
- Инварианты маппинга
- Ошибки и нормализация
- Примеры маппинга (DTO → Domain, Entity ↔ Domain)
- Быстрые ссылки (пути)

## Слои моделей

- DTO (транспорт, суффикс DTO) — только Data-слой:
  - Расположение: `shared/src/commonMain/kotlin/ru/izhxx/aichallenge/data/model/*DTO.kt`
  - Примеры: `ChatMessageDTO`, `LLMChatRequestDTO`, `LLMChatResponseDTO`, `UsageDTO`

- Entity (персистентность/БД):
  - Расположение: `shared/src/commonMain/kotlin/ru/izhxx/aichallenge/data/database/entity/*`
  - Используются DAO для CRUD; не выходят за пределы Data.

- Domain (бизнес-логика, чистые модели):
  - Расположение: `shared/src/commonMain/kotlin/ru/izhxx/aichallenge/domain/model/**`
  - Примеры: `LLMResponse`, `LLMChoice`, `LLMUsage`, `LLMMessage`, `DialogInfo`, `ChatMetrics`, `DomainException`

- UI (модели представления, не Domain!):
  - Расположение: `composeApp/src/commonMain/kotlin/ru/izhxx/aichallenge/features/**/presentation/model/*`
  - Примеры: `ChatUiState`, `ChatUiMessage`, `MessageContent`, `MessageMetadata`

## Инварианты маппинга

- DTO не пересекают границу Presentation/Domain. Маппинг выполняется в Data-слое, как extension-функции:
  - `fun SomeDTO.toDomain(): SomeDomain`
  - `fun SomeDomain.toDto(): SomeDTO` (если требуется обратная трансформация)

- Entity используются только для локального хранения. Маппинг с Domain — в Data-слое:
  - `fun SomeEntity.toDomain(): SomeDomain`
  - `fun SomeDomain.toEntity(...): SomeEntity`

- Nullability и дефолты задаются в мапперах, чтобы доменные модели были максимально безопасны:
  - Пример: пустые строки/списки, безопасные значения дат/счётчиков.

- Валидируйте и нормализуйте данные на границе слоя (DTO→Domain, Entity↔Domain). В UI — только валидные доменные/презентационные модели.

## Ошибки и нормализация

- Низкоуровневые (Data): 
  - `RequestError` (транспорт/таймауты/IO), `ApiError` (бизнес-ошибка бэкенда при валидном ответе)
  - Расположение: `shared/src/commonMain/kotlin/ru/izhxx/aichallenge/data/error/*`

- Доменная форма (единственная для верхних слоёв):
  - `DomainException` — `shared/src/commonMain/kotlin/ru/izhxx/aichallenge/domain/model/error/DomainException.kt`

- Все внешние вызовы оборачиваются в `safeApiCall { ... }`:
  - Конвертирует любые исключения в `DomainException` с user-friendly сообщением.
  - Расположение: `shared/src/commonMain/kotlin/ru/izhxx/aichallenge/common/SafeApiCall.kt`

## Примеры маппинга

DTO → Domain (сеть):
```kotlin
// DTO (Data)
@Serializable
data class LLMChatResponseDTO(
    val choices: List<ChoiceDTO> = emptyList(),
    val usage: UsageDTO? = null
)

@Serializable
data class ChoiceDTO(
    val index: Int,
    val message: ChatMessageDTO
)

@Serializable
data class ChatMessageDTO(
    val role: String,
    val content: String
)

@Serializable
data class UsageDTO(
    val prompt_tokens: Int? = null,
    val completion_tokens: Int? = null
)

// Domain (чистые модели)
data class LLMResponse(
    val choices: List<LLMChoice>,
    val usage: LLMUsage?
)

data class LLMChoice(
    val index: Int,
    val message: LLMMessage
)

data class LLMMessage(
    val role: MessageRole,
    val content: String
)

data class LLMUsage(
    val prompt: Int,
    val completion: Int
)

// Mapping (Data-слой)
fun LLMChatResponseDTO.toDomain(): LLMResponse =
    LLMResponse(
        choices = choices.map { it.toDomain() },
        usage = usage?.toDomain()
    )

fun ChoiceDTO.toDomain(): LLMChoice =
    LLMChoice(
        index = index,
        message = message.toDomain()
    )

fun ChatMessageDTO.toDomain(): LLMMessage =
    LLMMessage(
        role = MessageRole.from(role), // безопасная конверсия строки в enum
        content = content.ifBlank { "—" }
    )

fun UsageDTO.toDomain(): LLMUsage =
    LLMUsage(
        prompt = prompt_tokens ?: 0,
        completion = completion_tokens ?: 0
    )
```

Entity ↔ Domain (локальное хранилище):
```kotlin
// Entity (Data)
data class MessageEntity(
    val id: String,
    val dialogId: String,
    val content: String,
    val createdAt: Long
)

// Domain
data class LLMMessage(
    val role: MessageRole,
    val content: String
)

// Mapping (пример схемы)
fun MessageEntity.toDomain(): LLMMessage =
    LLMMessage(
        role = MessageRole.User, // роль может храниться отдельно; пример упрощён
        content = content
    )

fun LLMMessage.toEntity(dialogId: String, id: String, createdAt: Long): MessageEntity =
    MessageEntity(
        id = id,
        dialogId = dialogId,
        content = content,
        createdAt = createdAt
    )
```

Ошибки (Data → Domain):
```kotlin
suspend fun loadData(): DomainModel = safeApiCall {
    val dto: SomeDTO = api.get().body()
    dto.toDomain()
}
// Любые исключения/RequestError/ApiError будут нормализованы в DomainException.
```

## Быстрые ссылки (пути)

- DTO: `shared/src/commonMain/kotlin/ru/izhxx/aichallenge/data/model/*DTO.kt`
- Ошибки Data: `shared/src/commonMain/kotlin/ru/izhxx/aichallenge/data/error/*`
- Domain модели: `shared/src/commonMain/kotlin/ru/izhxx/aichallenge/domain/model/**`
- БД/DAO/Entity: `shared/src/commonMain/kotlin/ru/izhxx/aichallenge/data/database/{dao,entity}`
- Safe API: `shared/src/commonMain/kotlin/ru/izhxx/aichallenge/common/SafeApiCall.kt`
- Примеры UI-моделей: `composeApp/src/commonMain/kotlin/ru/izhxx/aichallenge/features/**/presentation/model/*`
