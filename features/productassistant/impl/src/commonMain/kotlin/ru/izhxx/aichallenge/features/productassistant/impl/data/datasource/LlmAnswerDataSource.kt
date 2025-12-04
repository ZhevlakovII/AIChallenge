package ru.izhxx.aichallenge.features.productassistant.impl.data.datasource

/**
 * Data source for generating answers using LLM
 */
interface LlmAnswerDataSource {
    /**
     * Generate answer using LLM with provided system and user prompts
     *
     * @param systemPrompt System-level instructions for the LLM
     * @param userPrompt User-level prompt containing the actual data to analyze
     * @return Result containing LLM response as String
     */
    suspend fun generateAnswer(
        systemPrompt: String,
        userPrompt: String
    ): Result<String>
}
