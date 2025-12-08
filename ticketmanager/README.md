# Ticket Manager

Мини-приложение для управления тикетами поддержки с интеграцией LLM-ассистента.

## Возможности

### 1. Экран входа
- Автоматический вход в систему (замокирован)
- Логин: `admin`, Пароль: `password`

### 2. Список тикетов
- Просмотр всех тикетов поддержки
- Фильтрация по статусу (открыт, в работе, решён)
- Фильтрация по тегам (auth, network, settings, bug, feature и др.)
- Переход к деталям тикета
- Обновление списка

### 3. Детали тикета
- Просмотр полной информации о тикете
- Изменение статуса (open → in_progress → resolved)
- Добавление комментариев
- История изменений

### 4. LLM Ассистент
Чат-интерфейс для работы с тикетами через естественный язык:

- **Получить список тикетов**: "Покажи все открытые тикеты"
- **Фильтрация**: "Найди тикеты с тегом 'auth'"
- **Изменение статуса**: "Измени статус тикета ticket-001 на 'в работе'"
- **Добавление комментариев**: "Добавь комментарий к тикету ticket-002: 'Проблема решена'"
- **Комплексные операции**: "Измени статус тикета ticket-003 на 'решён' и добавь комментарий 'Обновление установлено'"

## Архитектура

Приложение использует:
- **MVI** (Model-View-Intent) для управления состоянием UI
- **Clean Architecture** с разделением на слои Domain/Data/Presentation
- **Koin** для dependency injection
- **Compose Multiplatform** для UI
- **MCP Server** (instances/servers/mcp/support) для работы с данными тикетов

## Запуск приложения

### Desktop (JVM)

```bash
# Запустить MCP сервер поддержки
./gradlew :instances:servers:mcp:support:run

# В другом терминале запустить приложение
./gradlew :ticketmanager:run
```

### Android

```bash
# Запустить MCP сервер поддержки
./gradlew :instances:servers:mcp:support:run

# Собрать и установить APK
./gradlew :ticketmanager:assembleDebug
```

## Структура данных

Тикеты хранятся в файле `docs/support-tickets.json`:

```json
{
  "tickets": [
    {
      "id": "ticket-001",
      "userId": "user-123",
      "title": "Проблема с авторизацией",
      "description": "Не могу войти в систему",
      "status": "open",
      "createdAt": "2025-01-15T10:30:00Z",
      "updatedAt": "2025-01-15T10:30:00Z",
      "assignedTo": null,
      "comments": [],
      "tags": ["auth", "bug"]
    }
  ]
}
```

## MCP Tools

MCP сервер support предоставляет следующие инструменты:

1. `support.list_tickets` - Получить список тикетов с фильтрацией
2. `support.get_ticket` - Получить конкретный тикет по ID
3. `support.create_ticket` - Создать новый тикет
4. `support.update_ticket_status` - Обновить статус тикета
5. `support.add_comment` - Добавить комментарий к тикету

## Технологии

- **Kotlin Multiplatform** 2.2.21
- **Compose Multiplatform** 1.9.3
- **Ktor** 3.3.3 (MCP Server)
- **Koin** 4.1.1 (DI)
- **Kotlinx Coroutines** 1.10.2
- **Kotlinx Serialization** 1.9.0
- **Kotlinx DateTime**

## Навигация

Приложение использует Jetpack Compose Navigation:

- `/login` - Экран входа
- `/ticketList` - Список тикетов
- `/ticketDetails/{ticketId}` - Детали тикета
- `/llmAssistant` - LLM ассистент

После изменений в LLM ассистенте, при возврате к списку тикетов происходит автоматическое обновление.
