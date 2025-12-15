package ru.izhxx.aichallenge.instruments.user.profile.model

/**
 * Профиль пользователя для персонализации взаимодействия с LLM.
 * Содержит информацию о пользователе, которая будет включена в системный промпт.
 *
 * @property name Имя пользователя (опционально)
 * @property profession Профессия/роль пользователя (опционально)
 * @property experienceLevel Уровень опыта (опционально)
 * @property enabled Включить персонализацию (по умолчанию false)
 */
data class UserProfile(
    val name: String? = null,
    val profession: String? = null,
    val experienceLevel: String? = null,
    val enabled: Boolean = false
) {
    /**
     * Проверяет, заполнен ли профиль (хотя бы одно поле)
     */
    fun isNotEmpty(): Boolean {
        return !name.isNullOrBlank() ||
                !profession.isNullOrBlank() ||
                !experienceLevel.isNullOrBlank()
    }

    /**
     * Форматирует профиль в текст для добавления в системный промпт
     */
    fun toPromptText(): String? {
        if (!enabled || !isNotEmpty()) return null

        val parts = mutableListOf<String>()

        if (!name.isNullOrBlank()) {
            parts.add("Меня зовут $name")
        }

        if (!profession.isNullOrBlank() && !experienceLevel.isNullOrBlank()) {
            parts.add("я $profession с опытом $experienceLevel")
        } else if (!profession.isNullOrBlank()) {
            parts.add("я $profession")
        } else if (!experienceLevel.isNullOrBlank()) {
            parts.add("мой уровень опыта: $experienceLevel")
        }

        return if (parts.isNotEmpty()) {
            parts.joinToString(", ") + "."
        } else {
            null
        }
    }
}
