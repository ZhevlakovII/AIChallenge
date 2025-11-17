# Шаблон: UseCase (интерфейс и реализация)

Назначение: стандартный каркас UseCase в стиле Чистой архитектуры. Интерфейс — в Domain, реализация — в Data (shared) либо локально в фиче (если не переиспользуется). Совместим с KMP, Koin, Coroutines. Соответствует правилам DTO-изоляции и DIP.

См. также:
- docs/human/FeatureDevelopmentGuide.md
- docs/human/CodingStandards.md
- docs/llm/file_paths_policy.md
- docs/llm/code_generation_rules.md

Рекомендуемое размещение:
- Интерфейс (общий): `shared/src/commonMain/kotlin/ru/izhxx/aichallenge/domain/usecase/<Action><Entity>UseCase.kt`
- Реализация (общая): `shared/src/commonMain/kotlin/ru/izhxx/aichallenge/data/usecase/<Action><Entity>UseCaseImpl.kt`
- Локально для фичи (если применимо): 
  - интерфейс/реализация в `composeApp/src/commonMain/kotlin/ru/izhxx/aichallenge/features/<feature>/domain/usecase/`

## Интерфейс UseCase (Domain)

```kotlin
package ru.izhxx.aichallenge.domain.usecase

/**
 * UseCase <Action><Entity>.
 *
 * Выполняет бизнес-операцию над доменными моделями, не зная об источниках данных.
 * Не содержит платформенных зависимостей и инфраструктурного кода.
 *
 * Примеры имен:
 * - SendMessageUseCase
 * - FetchDialogsUseCase
 * - UpdateProviderSettingsUseCase
 */
interface <Action><Entity>UseCase {
    /**
     * Выполняет операцию <Action> для <Entity>.
     *
     * @param params Параметры операции (при необходимости). Замените на конкретный тип или используйте Unit.
     * @return Доменный результат операции
     * @throws ru.izhxx.aichallenge.domain.model.error.DomainException при ошибках
     */
    suspend operator fun invoke(params: <ParamsType>): <ResultType>
}
```

Пример без параметров:
```kotlin
interface <Action><Entity>UseCase {
    suspend operator fun invoke(): <ResultType>
}
```

## Реализация UseCase (Data)

```kotlin
package ru.izhxx.aichallenge.data.usecase

import ru.izhxx.aichallenge.domain.repository.<Entity>Repository
import ru.izhxx.aichallenge.domain.usecase.<Action><Entity>UseCase

/**
 * Реализация UseCase <Action><Entity>.
 *
 * Делегирует работу доменному репозиторию. Вся IO/сетевая логика и нормализация ошибок
 * инкапсулированы в слое Data (репозитории/источники), включая safeApiCall.
 */
class <Action><Entity>UseCaseImpl(
    private val repository: <Entity>Repository
) : <Action><Entity>UseCase {

    override suspend fun invoke(params: <ParamsType>): <ResultType> {
        // Делегирование доменному репозиторию, без DTO/платформенных зависимостей.
        return repository.<action>(params)
    }
}
```

Пример без параметров:
```kotlin
class <Action><Entity>UseCaseImpl(
    private val repository: <Entity>Repository
) : <Action><Entity>UseCase {
    override suspend fun invoke(): <ResultType> = repository.<action>()
}
```

## DI (Koin) — регистрация

```kotlin
// В модуле фичи или общем DI-модуле shared:
val <featureOrShared>Module = org.koin.dsl.module {
    factory<<Action><Entity>UseCase> { <Action><Entity>UseCaseImpl(get()) }
}
```

Если реализация переиспользуется несколькими фичами — регистрируйте в общих DI-модулях (`shared/src/commonMain/.../di`).

## Правила и рекомендации

- Интерфейс UseCase всегда в Domain. Реализация — в Data (или в фиче, если строго локальна).
- Не используйте в UseCase платформенные типы и зависимости (Dispatchers, Context, Android API и т.п.).
  - Если требуется диспетчер — инжектируйте абстракцию диспетчера в Data/репозитории; UseCase остаётся чистым.
- Обработка ошибок на границе Data через `safeApiCall { ... }`, конвертация в `DomainException`.
  - UseCase не занимается нормализацией ошибок, только пробрасывает доменную форму.
- DTO/Entity запрещены в сигнатурах UseCase. Только доменные модели/типы.
- Именование UseCase — Глагол+Существительное (например, `SendMessageUseCase`).

## Тестирование

Юнит-тесты интерфейс/реализация:
- Размещение: `shared/src/commonTest/kotlin/...`
- Подход:
  - Мок/фейк `Repository` (Domain контракт).
  - Тест: при успешном ответе репозитория — ожидаемый доменный результат.
  - Тест: при ошибке — проброс `DomainException`.

Пример тестовой заготовки:
```kotlin
class <Action><Entity>UseCaseImplTest {
    private class FakeRepository : <Entity>Repository {
        var result: Result<<ResultType>> = Result.success(/*...*/)
        override suspend fun <action>(params: <ParamsType>): <ResultType> = result.getOrThrow()
    }

    @kotlin.test.Test
    fun success_returnsResult() = kotlinx.coroutines.test.runTest {
        val repo = FakeRepository().apply { result = Result.success(/* expected */) }
        val useCase = <Action><Entity>UseCaseImpl(repo)

        val actual = useCase(/* params */)
        // assertEquals(expected, actual)
    }

    @kotlin.test.Test
    fun failure_throwsDomainException() = kotlinx.coroutines.test.runTest {
        val repo = FakeRepository().apply { 
            result = Result.failure(ru.izhxx.aichallenge.domain.model.error.DomainException("error")) 
        }
        val useCase = <Action><Entity>UseCaseImpl(repo)

        kotlin.test.assertFailsWith<ru.izhxx.aichallenge.domain.model.error.DomainException> {
            useCase(/* params */)
        }
    }
}
```

## Встраивание во ViewModel

- Инжектируйте UseCase в VM через Koin.
- Вызывайте в `viewModelScope.launch { ... }`, обновляйте `State` через `_state.update { ... }`.
- Исключения ловите в VM: `onFailure { e -> e as? DomainException ... }` и кладите в поле `error` в состоянии.

Ссылки:
- Error Handling: docs/human/ErrorHandling.md
- Feature VM шаблон: docs/llm/templates/FeatureViewModel.kt.md
- DI модуль фичи: docs/llm/templates/FeatureModule.kt.md
