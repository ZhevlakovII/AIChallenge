package ru.izhxx.aichallenge.core.ui.mvi.model

/**
 * Интерфейс‑маркер для намерений (Intent) в MVI для UI‑слоя.
 *
 * Назначение:
 * - Intent описывает намерение пользователя или внешнего источника изменить состояние экрана
 *   (например, нажатие кнопки, ввод текста, жест, событие из системы).
 * - Intent — входная точка в конвейер MVI. View/Compose отправляет его в [accept] у MVI‑ViewModel.
 *
 * Рекомендации по моделированию:
 * - Объявляйте Intent как `sealed interface`/`sealed class` на уровне конкретного экрана/фичи,
 *   чтобы типобезопасно исчерпывающе описать все события.
 * - Делайте Intent неизменяемыми (immutability), без побочных эффектов.
 * - Старайтесь не смешивать в Intent данные, получаемые асинхронно — Intent должен быть фактом
 *   от UI/источника, а не результатом выполнения.
 *
 * Поток данных (упрощённо):
 * UI(View/Compose) → Intent → MVI‑ViewModel.accept(intent) → Executor (исполнение)
 * → Result/Effect → Reducer(State × Result → State) → Обновлённый State → Рендер UI.
 *
 * Пример:
 * ```
 * sealed interface SearchIntent : MviIntent {
 *   data class QueryChanged(val value: String) : SearchIntent
 *   object SubmitClicked : SearchIntent
 *   object Retry : SearchIntent
 * }
 * ```
 */
public interface MviIntent
