package ru.izhxx.aichallenge.core.errors

/**
 * Резервированные ключи для метаданных.
 */
@Deprecated("Миграция на создание через конструктор")
object MetadataKeys {
    val CORRELATION_ID = MetadataKey("correlation_id")
    val REQUEST_ID = MetadataKey("request_id")
    val OPERATION = MetadataKey("operation")
    val FEATURE = MetadataKey("feature")
    val MODULE = MetadataKey("module")

    // Network/HTTP
    val URL = MetadataKey("url")
    val ENDPOINT = MetadataKey("endpoint")
    val METHOD = MetadataKey("method")
    val HTTP_STATUS = MetadataKey("http_status")
    val RETRY_AFTER_SECONDS = MetadataKey("retry_after_seconds")

    // Storage/IO
    val FILE_PATH = MetadataKey("file_path")
    val SQL_STATE = MetadataKey("sql_state")
    val CONSTRAINT = MetadataKey("constraint")

    // Validation
    val VALIDATION_FIELD = MetadataKey("validation_field")
    val VALIDATION_RULE = MetadataKey("validation_rule")

    // Timeout
    val TIMEOUT_MILLIS = MetadataKey("timeout_millis")

    // Origin (библиотека/слой)
    val ORIGIN = MetadataKey("origin")
}
