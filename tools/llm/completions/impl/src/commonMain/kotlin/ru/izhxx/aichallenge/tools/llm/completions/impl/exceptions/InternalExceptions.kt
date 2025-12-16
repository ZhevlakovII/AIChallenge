package ru.izhxx.aichallenge.tools.llm.completions.impl.exceptions

internal class UnknownBodyException(body: String?, originalException: Exception?) : RuntimeException(
    message = "Unknown body, body ${body ?: "empty or null body"}", cause = originalException
)

internal class FailureRequestException(endpoint: String) : RuntimeException(
    message = "Failure request to $endpoint"
)