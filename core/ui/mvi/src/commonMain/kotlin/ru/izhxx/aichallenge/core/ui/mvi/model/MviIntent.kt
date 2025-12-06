package ru.izhxx.aichallenge.core.ui.mvi.model

import ru.izhxx.aichallenge.core.ui.mvi.runtime.MviViewModel

/**
 * Интерфейс‑маркер для намерений (Intent) в MVI для UI‑слоя.
 *
 * Назначение:
 * - Intent описывает намерение пользователя или внешнего источника изменить состояние экрана
 *   (например, нажатие кнопки, ввод текста, жест, событие из системы).
 * - Intent — входная точка в конвейер MVI. View/Compose отправляет его в [MviViewModel.accept].
 *
 * Требования по моделированию:
 * - Объявляйте Intent как `sealed interface`/`sealed class` на уровне конкретного экрана/фичи,
 *   чтобы типобезопасно исчерпывающе описать все события.
 * - Делайте Intent неизменяемыми (immutability), без побочных эффектов.
 * - Не смешивайте в Intent данные, получаемые асинхронно — Intent должен быть фактом
 *   от UI/источника, а не результатом выполнения.
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
interface MviIntent
