# Шаблон: FeatureViewModel (MVI, StateFlow, Koin)

Назначение: быстрый старт ViewModel для новой фичи в стиле MVI. Совместим с KMP+CMP, Koin, Coroutines/StateFlow и правилами проекта.

См. также:
- docs/llm/file_paths_policy.md
- docs/human/FeatureDevelopmentGuide.md
- docs/human/CodingStandards.md
- docs/human/ErrorHandling.md

Рекомендуемое размещение:
- composeApp/src/commonMain/kotlin/ru/izhxx/aichallenge/features/<feature>/presentation/<Feature>ViewModel.kt
- Модели состояния/событий: composeApp/.../features/<feature>/presentation/model/

```kotlin
package ru.izhxx.aichallenge.features.<feature>.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.izhxx.aichallenge.domain.model.error.DomainException
// Импортируйте доменные use case/контракты:
import ru.izhxx.aichallenge.features.<feature>.domain.usecase.<Action><Entity>UseCase
// или если use case общий:
import ru.izhxx.aichallenge.domain.usecase.<Action><Entity>UseCase

/**
 * ViewModel фичи <Feature> в стиле MVI.
 *
 * Состояние представлено неизменяемой моделью [<Feature>State],
 * события описаны в [<Feature>Event].
 *
 * Правила:
 * - Обновление состояния только через _state.update { it.copy(...) }
 * - Внешние исключения должны приходить как DomainException (см. safeApiCall)
 */
class <Feature>ViewModel(
    // Инжектируйте зависимости через Koin (см. DI-модуль фичи)
    private val <action><entity>UseCase: <Action><Entity>UseCase
) : ViewModel() {

    private val _state = MutableStateFlow(<Feature>State())
    val state: StateFlow<<Feature>State> = _state.asStateFlow()

    /**
     * Точка входа для событий UI/системы.
     */
    fun processEvent(event: <Feature>Event) {
        when (event) {
            is <Feature>Event.Refresh -> refresh()
            is <Feature>Event.Select -> onSelect(event.id)
            is <Feature>Event.Submit -> submit(event.payload)
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            runCatching {
                // Вызовите доменный use case и верните доменную модель
                <action><entity>UseCase(/* параметры, если нужны */)
            }.onSuccess { result ->
                // Преобразуйте доменные модели в UI-модели при необходимости (mapper в presentation-слое)
                _state.update { it.copy(isLoading = false, data = result /* mapped */) }
            }.onFailure { throwable ->
                val domainError = throwable as? DomainException
                    ?: DomainException(message = "Не удалось обновить данные", cause = throwable)
                _state.update { it.copy(isLoading = false, error = domainError) }
            }
        }
    }

    private fun onSelect(id: String) {
        // Обновите состояние/вызовите эффект навигации через внешние колбэки, если требуется
        _state.update { it.copy(selectedId = id) }
    }

    private fun submit(payload: String) {
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, error = null) }
            runCatching {
                <action><entity>UseCase(/* payload */)
            }.onSuccess {
                _state.update { it.copy(isSubmitting = false, submitSuccess = true) }
            }.onFailure { throwable ->
                val domainError = throwable as? DomainException
                    ?: DomainException(message = "Ошибка отправки", cause = throwable)
                _state.update { it.copy(isSubmitting = false, error = domainError) }
            }
        }
    }
}
```

Модели состояния и событий:
```kotlin
package ru.izhxx.aichallenge.features.<feature>.presentation.model

/**
 * Состояние экрана <Feature>.
 */
data class <Feature>State(
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val submitSuccess: Boolean = false,
    val selectedId: String? = null,
    val data: List<Any> = emptyList(), // замените на конкретную UI-модель
    val error: ru.izhxx.aichallenge.domain.model.error.DomainException? = null
)

/**
 * События экрана <Feature>.
 */
sealed class <Feature>Event {
    data object Refresh : <Feature>Event()
    data class Select(val id: String) : <Feature>Event()
    data class Submit(val payload: String) : <Feature>Event()
}
```

Фрагмент UI (подписка на состояние):
```kotlin
@Composable
fun <Feature>Screen(
    viewModel: <Feature>ViewModel = koinViewModel()
) {
    // Используем lifecycle‑aware подписку
    val state by viewModel.state.collectAsStateWithLifecycle()

    // TODO: отрисуйте состояние (loading/data/error)
    // Пример:
    // if (state.isLoading) LoadingIndicator()
    // state.error?.let { ErrorBanner(it.message ?: "Ошибка") }
    // LazyColumn { items(state.data) { /* ... */ } }

    // Отправка событий:
    // Button(onClick = { viewModel.processEvent(<Feature>Event.Refresh) }) { Text("Обновить") }
}
```

DI-модуль фичи (пример регистрации):
```kotlin
val <feature>Module = module {
    // UseCase
    factory<<Action><Entity>UseCase> { <Action><Entity>UseCaseImpl(get()) }
    // ViewModel
    viewModel { <Feature>ViewModel(<action><entity>UseCase = get()) }
}
```

Примечания:
- Вся сетевая/IO-логика и маппинг DTO должны находиться в Data-слое. В VM используйте только доменные use case/контракты.
- Исключения в VM должны быть доменной формы (DomainException). Оборачивайте внешние вызовы в Data-слое через safeApiCall.
- Для одноразовых эффектов используйте отдельный SharedFlow (по необходимости).
