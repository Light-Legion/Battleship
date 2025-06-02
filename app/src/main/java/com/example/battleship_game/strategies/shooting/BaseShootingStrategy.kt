package com.example.battleship_game.strategies.shooting

/**
 * Абстрактная базовая реализация ShootingStrategy.
 * Обрабатывает общую логику:
 * - Отслеживание обстрелянных клеток (tried)
 * - Валидацию координат
 * - Шаблонный метод для обработки результатов
 */
abstract class BaseShootingStrategy : ShootingStrategy {
    // Матрица 10x10: true = клетка уже обстреляна
    protected val tried = Array(10) { BooleanArray(10) { false } }

    // Последняя выбранная клетка для выстрела
    private var lastShot: Pair<Int, Int>? = null

    /**
     * Финальная реализация выбора следующего выстрела.
     * Гарантирует валидность и уникальность клетки.
     */
    final override fun getNextShot(): Pair<Int, Int> {
        while (true) {
            val (x, y) = computeNextShot()
            if (isValidCell(x, y) && !tried[y][x]) {
                lastShot = x to y
                return x to y
            }
        }
    }

    /**
     * Финальная обработка результата выстрела.
     * Помечает клетку как обстрелянную и делегирует логику наследникам.
     */
    final override fun setShotResult(hit: Boolean, sunk: Boolean) {
        lastShot?.let { (x, y) ->
            if (isValidCell(x, y)) {
                tried[y][x] = true
            }
        }
        onShotResult(lastShot, hit, sunk)
    }

    /**
     * Проверяет, что координаты находятся в пределах игрового поля (0-9).
     */
    protected fun isValidCell(x: Int, y: Int): Boolean {
        return x in 0..9 && y in 0..9
    }

    /**
     * "Сырая" логика выбора следующей клетки (без учета tried).
     * Должна быть реализована наследниками.
     */
    protected abstract fun computeNextShot(): Pair<Int, Int>

    /**
     * Обработка результата выстрела для обновления внутреннего состояния.
     * @param lastShot - координаты последнего выстрела (x, y)
     * @param hit - попали ли в корабль
     * @param sunk - потопили ли корабль
     */
    protected open fun onShotResult(
        lastShot: Pair<Int, Int>?,
        hit: Boolean,
        sunk: Boolean
    ) = Unit
}