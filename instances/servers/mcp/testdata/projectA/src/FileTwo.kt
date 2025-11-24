package testdata.projectA

// FIXME [bug] P1: Неправильная локализация сообщений об ошибках
// TODO [feature] Добавить поддержку импорта из CSV (soon)
// NOTE: Эта строка не должна попадать в выборку по TODO/FIXME

data class User(val id: Long, val name: String)

/**
 * Пример функции, чтобы были разные комментарии и контекст.
 */
fun validate(user: User?): Boolean {
    // TODO [refactor] P2: Разделить валидацию на мелкие функции (later, low)
    if (user == null) return false
    // fixme: старый формат имени — убрать к следующему релизу
    return user.name.isNotBlank()
}

/*
 * TODO [doc]: Описать формат CSV в wiki
 */
fun importFromCsv(input: String): List<User> {
    // Имитация импорта
    return input.lines().filter { it.isNotBlank() }.mapIndexed { idx, line ->
        // TODO [test]: покрыть невалидные строки
        User(id = idx.toLong(), name = line.trim())
    }
}
