package ru.izhxx.aichallenge.features.pranalyzer.impl.data.datasource

/**
 * Data source for analyzing code using LLM
 *
 * Provides methods to send prompts to LLM and receive analysis results.
 * Uses custom system prompts specifically designed for code review tasks.
 */
interface LlmAnalysisDataSource {

    /**
     * Analyzes code using LLM with custom system and user prompts
     *
     * @param systemPrompt System-level instructions for the LLM (defines task and context)
     * @param userPrompt User-level prompt containing the actual data to analyze
     * @return Result containing LLM response as String (expected to be JSON) or error
     */
    suspend fun analyzeCode(
        systemPrompt: String,
        userPrompt: String
    ): Result<String>
}
