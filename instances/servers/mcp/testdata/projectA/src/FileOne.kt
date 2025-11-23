package testdata.projectA

// TODO [bug] P0: Исправить крэш при входе с пустым паролем (urgent, ASAP)
// FIXME [refactor]: Упростить обработку ошибок в network слое
// TODO [doc] low: Добавить KDoc ко всем публичным методам

@Suppress("UNUSED_PARAMETER")
fun login(username: String, password: String): Boolean {
    // Имитация неочевидной логики
    if (username.isBlank()) return false
    // TODO [test]: Добавить параметризованные тесты для login()
    return password.isNotEmpty()
}

fun compute(a: Int, b: Int): Int {
    // TODO [perf]: Оптимизировать цикл, потенциально O(n^2)
    var acc = 0
    repeat(a) { i ->
        repeat(b) { j ->
            acc += (i + j) % 3
        }
    }
    return acc
}
