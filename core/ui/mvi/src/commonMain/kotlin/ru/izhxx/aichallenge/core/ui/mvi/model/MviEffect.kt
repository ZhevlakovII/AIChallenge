package ru.izhxx.aichallenge.core.ui.mvi.model

/**
 * Интерфейс‑маркер для одноразовых побочных эффектов (Effect) в MVI.
 *
 * Назначение:
 * - Effect представляет собой одноразовое событие, которое не должно кэшироваться в [MviState]
 *   и повторно воспроизводиться при пересоздании подписки. Типичные примеры: навигация,
 *   показ Snackbar/Toast/диалога, хаптик, Share Sheet.

 * Требования по моделированию:
 * - Объявляйте Effect как `sealed interface`/`sealed class` на уровне конкретного экрана/фичи.
 * - Не кодируйте в Effect то, что должно быть частью состояния (индикаторы загрузки/ошибки для UI).
 * - Эмитите Effect в результате обработки Result, но доставляйте в UI
 *   как одноразовый поток событий (например, через `SharedFlow`/`Channel`, наружу — как `Flow`).
 *
 * Пример:
 * ```
 * sealed interface SearchEffect : MviEffect {
 *   data class ShowMessage(val text: String) : SearchEffect
 *   data class NavigateToDetails(val id: String) : SearchEffect
 * }
 * ```
 */
interface MviEffect
