package ru.izhxx.aichallenge.core.logger

import ru.izhxx.aichallenge.core.logger.Tag.Companion.MAX_LENGTH
import kotlin.jvm.JvmInline

/**
 * Короткий тег логирования, общий для всех платформ.
 *
 * Инкапсулирует строковое значение с ограничением длины [MAX_LENGTH] (23 символа)
 * для совместимости с Android Log и единообразного форматирования.
 *
 * Создавайте через [Tag.of] (жёсткая проверка длины) или [Tag.ofTruncated] (безопасное усечение).
 */
@JvmInline
value class Tag private constructor(val value: String) {

    override fun toString(): String = value

    /**
     * Фабричные методы создания [Tag] и связанные константы.
     */
    companion object {

        /**
         * Максимально допустимая длина тега для всех платформ.
         * Используйте в IDE/подсказках: [Tag.MAX_LENGTH].
         */
        const val MAX_LENGTH: Int = 23

        /**
         * Создаёт тег, если он не пустой и не длиннее [MAX_LENGTH].
         * В случае превышения длины бросает [IllegalArgumentException],
         * тем самым явно подсвечивая ограничение на этапе разработки.
         */
        fun of(raw: String): Tag {
            val value = raw.trim()
            require(value.isNotEmpty()) { "Tag must not be blank" }
            require(value.length <= MAX_LENGTH) {
                "Tag length (${value.length}) exceeds MAX_LENGTH=$MAX_LENGTH. " +
                    "Use Tag.ofTruncated(...) if truncation is acceptable."
            }
            return Tag(value)
        }

        /**
         * Безопасное создание тега со встроенным усечением до [MAX_LENGTH].
         * Можно передать onTruncate для явного логирования факта усечения.
         */
        fun ofTruncated(
            raw: String,
            onTruncate: ((original: String, truncated: String) -> Unit)? = null
        ): Tag {
            val value = raw.trim()
            require(value.isNotEmpty()) { "Tag must not be blank" }
            val truncatedValue = if (value.length <= MAX_LENGTH) {
                value
            } else {
                value.take(MAX_LENGTH).also {
                    onTruncate?.invoke(value, it)
                }
            }
            return Tag(truncatedValue)
        }
    }
}
