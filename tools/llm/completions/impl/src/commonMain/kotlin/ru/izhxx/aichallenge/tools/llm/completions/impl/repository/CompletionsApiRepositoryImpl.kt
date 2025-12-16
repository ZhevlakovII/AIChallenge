package ru.izhxx.aichallenge.tools.llm.completions.impl.repository

import io.ktor.client.HttpClient
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import ru.izhxx.aichallenge.core.errors.AppError
import ru.izhxx.aichallenge.core.errors.ErrorRetry
import ru.izhxx.aichallenge.core.errors.ErrorSeverity
import ru.izhxx.aichallenge.core.result.AppResult
import ru.izhxx.aichallenge.core.safecall.suspendedSafeCall
import ru.izhxx.aichallenge.tools.llm.completions.api.model.Message
import ru.izhxx.aichallenge.tools.llm.completions.api.model.MessageRole
import ru.izhxx.aichallenge.tools.llm.completions.api.model.Usage
import ru.izhxx.aichallenge.tools.llm.completions.api.model.answer.Answer
import ru.izhxx.aichallenge.tools.llm.completions.api.model.answer.AnswerMessage
import ru.izhxx.aichallenge.tools.llm.completions.api.model.answer.Choice
import ru.izhxx.aichallenge.tools.llm.completions.api.repository.CompletionsApiRepository
import ru.izhxx.aichallenge.tools.llm.completions.impl.exceptions.FailureRequestException
import ru.izhxx.aichallenge.tools.llm.completions.impl.exceptions.UnknownBodyException
import ru.izhxx.aichallenge.tools.llm.completions.impl.model.request.McpFunctionRequestDTO
import ru.izhxx.aichallenge.tools.llm.completions.impl.model.request.McpToolRequestDTO
import ru.izhxx.aichallenge.tools.llm.completions.impl.model.request.MessageRequestDTO
import ru.izhxx.aichallenge.tools.llm.completions.impl.model.request.RequestDTO
import ru.izhxx.aichallenge.tools.llm.completions.impl.model.response.AnswerDTO
import ru.izhxx.aichallenge.tools.llm.config.model.ParametersConfig
import ru.izhxx.aichallenge.tools.llm.config.model.ProviderConfig
import ru.izhxx.aichallenge.tools.shared.mcp.model.McpCallFunction
import ru.izhxx.aichallenge.tools.shared.mcp.model.McpCallTool
import ru.izhxx.aichallenge.tools.shared.mcp.model.McpTool
import ru.izhxx.aichallenge.tools.shared.mcp.model.McpToolType

internal class CompletionsApiRepositoryImpl(
    private val httpClient: HttpClient,
    private val json: Json
) : CompletionsApiRepository {

    override suspend fun sendMessage(
        parametersConfig: ParametersConfig,
        providerConfig: ProviderConfig,
        messages: List<Message>,
        tools: List<McpTool>
    ): AppResult<Answer> {
        return suspendedSafeCall(
            throwableMapper = { t ->
                // TODO add more exceptions
                when (t) {
                    is UnknownBodyException -> AppError.DomainError(
                        severity = ErrorSeverity.Error,
                        retry = ErrorRetry.Forbidden,
                        cause = t.cause,
                        rawMessage = t.message,
                        metadata = mapOf(

                        )
                    )

                    else -> AppError.UnknownError(
                        severity = ErrorSeverity.Warning,
                        retry = ErrorRetry.Forbidden,
                        cause = t.cause,
                        rawMessage = t.message,
                        metadata = mapOf(

                        )
                    )
                }
            }
        ) {
            val answerDTO = sendMessageToLLM(
                request = RequestDTO(
                    messages = messages.map { message ->
                        MessageRequestDTO(
                            role = message.role.key,
                            content = message.content
                        )
                    },
                    model = providerConfig.model,
                    temperature = parametersConfig.temperature,
                    maxTokens = parametersConfig.maxTokens,
                    topK = parametersConfig.topK,
                    topP = parametersConfig.topP,
                    minP = parametersConfig.minP,
                    topA = parametersConfig.topA,
                    seed = parametersConfig.seed,
                    tools = tools.map { tool ->
                        McpToolRequestDTO(
                            type = tool.type.key,
                            function = McpFunctionRequestDTO(
                                name = tool.function.name,
                                description = tool.function.description,
                                parameters = tool.function.parameters?.toString()
                            )
                        )
                    }
                ),
                providerConfig = providerConfig
            )
            Answer(
                id = answerDTO.id,
                createdAt = answerDTO.createdAt,
                model = answerDTO.model,
                choices = answerDTO.choices.map { choiceDTO ->
                    Choice(
                        index = choiceDTO.index,
                        message = AnswerMessage(
                            role = MessageRole.parseRole(choiceDTO.message.role),
                            content = choiceDTO.message.content.orEmpty(),
                            toolCalls = choiceDTO.message.toolCalls?.map { mcpToolResponse ->
                                McpCallTool(
                                    type = McpToolType.parseType(mcpToolResponse.type),
                                    function = McpCallFunction(
                                        name = mcpToolResponse.function.name,
                                        arguments = mcpToolResponse.function.arguments?.let { arguments ->
                                            json.decodeFromString(arguments)
                                        }
                                    )
                                )
                            }.orEmpty()
                        )
                    )
                },
                usage = answerDTO.usage?.let { usageDTO ->
                    Usage(
                        promtTokens = usageDTO.promtTokens,
                        completionTokens = usageDTO.completionTokens,
                        totalTokens = usageDTO.totalTokens
                    )
                }
            )
        }
    }

    private suspend fun sendMessageToLLM(
        request: RequestDTO,
        providerConfig: ProviderConfig
    ): AnswerDTO {
        val response = httpClient.post {
            url(providerConfig.apiUrl.data)
            header(HttpHeaders.Authorization, HEADER_AUTH_BEARER + providerConfig.apiKey)
            setBody(request)
        }


        if (!response.status.isSuccess()) {
            throw FailureRequestException(
                endpoint = response.request.url.toString()
            )
        }

        try {
            return response.body<AnswerDTO>()
        } catch (e: NoTransformationFoundException) {
            throw UnknownBodyException(
                body = response.bodyAsText(),
                originalException = e
            )
        }
    }

    private companion object {
        private const val HEADER_AUTH_BEARER = "Bearer"
    }
}