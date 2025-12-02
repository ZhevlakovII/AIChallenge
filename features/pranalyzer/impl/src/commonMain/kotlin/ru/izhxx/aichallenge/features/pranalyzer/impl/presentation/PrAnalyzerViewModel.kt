package ru.izhxx.aichallenge.features.pranalyzer.impl.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import ru.izhxx.aichallenge.core.ui.mvi.runtime.MviReducer
import ru.izhxx.aichallenge.core.ui.mvi.runtime.MviViewModel
import ru.izhxx.aichallenge.features.pranalyzer.impl.presentation.model.AnalysisStage
import ru.izhxx.aichallenge.features.pranalyzer.impl.presentation.model.PrAnalyzerEffect
import ru.izhxx.aichallenge.features.pranalyzer.impl.presentation.model.PrAnalyzerIntent
import ru.izhxx.aichallenge.features.pranalyzer.impl.presentation.model.PrAnalyzerResult
import ru.izhxx.aichallenge.features.pranalyzer.impl.presentation.model.PrAnalyzerState
import kotlin.time.ExperimentalTime

/**
 * MVI ViewModel for PR Analyzer screen
 *
 * Orchestrates the MVI pipeline:
 * Intent → Executor → Result/Effect → Reducer → State → UI
 */
@ExperimentalTime
class PrAnalyzerViewModel(
    private val executor: PrAnalyzerExecutor
) : ViewModel(), MviViewModel<PrAnalyzerIntent, PrAnalyzerResult, PrAnalyzerState, PrAnalyzerEffect> {

    // Initial state
    private val initialState = PrAnalyzerState(
        prUrl = "",
        isLoading = false,
        loadingStage = AnalysisStage.IDLE,
        progress = 0f,
        prInfo = null,
        analysisReport = null,
        error = null,
        expandedIssueIds = emptySet(),
        isUrlValid = false,
        canAnalyze = false
    )

    // Mutable state for internal updates
    private val _state = MutableStateFlow(initialState)

    // Public immutable state for UI observation
    override val state: StateFlow<PrAnalyzerState> = _state.asStateFlow()

    // Channel for one-time effects
    private val _effects = Channel<PrAnalyzerEffect>(Channel.BUFFERED)

    // Public flow of effects for UI observation
    override val effects: Flow<PrAnalyzerEffect> = _effects.receiveAsFlow()

    // Pure reducer function for state transformation
    private val reducer = MviReducer<PrAnalyzerState, PrAnalyzerResult> { currentState, result ->
        when (result) {
            is PrAnalyzerResult.UrlValidated -> {
                currentState.copy(
                    prUrl = result.url,
                    isUrlValid = result.isValid,
                    canAnalyze = result.isValid && !currentState.isLoading,
                    error = null
                )
            }

            is PrAnalyzerResult.AnalysisStarted -> {
                currentState.copy(
                    isLoading = true,
                    loadingStage = AnalysisStage.IDLE,
                    progress = 0f,
                    error = null,
                    analysisReport = null,
                    canAnalyze = false
                )
            }

            is PrAnalyzerResult.ProgressUpdated -> {
                currentState.copy(
                    loadingStage = result.stage,
                    progress = result.progress
                )
            }

            is PrAnalyzerResult.PrInfoFetched -> {
                currentState.copy(
                    prInfo = result.prInfo
                )
            }

            is PrAnalyzerResult.ReportGenerated -> {
                currentState.copy(
                    analysisReport = result.report,
                    isLoading = false,
                    loadingStage = AnalysisStage.COMPLETED,
                    progress = 1.0f,
                    canAnalyze = currentState.isUrlValid
                )
            }

            is PrAnalyzerResult.AnalysisFailed -> {
                currentState.copy(
                    isLoading = false,
                    error = result.error,
                    canAnalyze = currentState.isUrlValid
                )
            }

            is PrAnalyzerResult.IssueExpanded -> {
                val expandedIds = currentState.expandedIssueIds.toMutableSet()
                if (result.issueId in expandedIds) {
                    expandedIds.remove(result.issueId)
                } else {
                    expandedIds.add(result.issueId)
                }
                currentState.copy(expandedIssueIds = expandedIds)
            }

            is PrAnalyzerResult.ErrorCleared -> {
                currentState.copy(error = null)
            }
        }
    }

    /**
     * Accepts user intents and processes them through the MVI pipeline
     */
    override fun accept(intent: PrAnalyzerIntent) {
        viewModelScope.launch {
            executor.execute(
                intent = intent,
                emitResult = { result ->
                    // Update state through reducer
                    _state.value = reducer.reduce(_state.value, result)
                },
                emitEffect = { effect ->
                    // Send one-time effect to channel
                    _effects.send(effect)
                }
            )
        }
    }
}
