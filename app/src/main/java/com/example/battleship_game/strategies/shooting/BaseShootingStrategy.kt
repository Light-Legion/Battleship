package com.example.battleship_game.strategies.shooting

/**
 * Абстрактная базовая реализация ShootingStrategy.
 * Обрабатывает общую логику:
 *  - Матрица tried[row][col]: true = клетка уже обстреляна
 *  - Валидация координат
 *  - Шаблонные методы для выбора и обработки результата выстрела
 */
abstract class BaseShootingStrategy : ShootingStrategy {
    // tried[row][col] = true, если эта клетка уже обстреляна
    protected val tried = Array(10) { BooleanArray(10) { false } }

    // Последняя выбранная клетка для выстрела (row, col)
    protected var lastShot: Pair<Int, Int>? = null

    /**
     * Финальная реализация выбора следующего выстрела.
     * Гарантирует, что вернёт неиспользованную клетку в формате (row, col).
     */
    final override fun getNextShot(): Pair<Int, Int> {
        // Пытаемся найти валидную (row, col) максимум за N попыток,
        // чтобы избежать теоретического зацикливания, если стратегия дала "неправильные" координаты.
        repeat(100) {
            val (r, c) = computeNextShot()
            if (isValidCell(r, c) && !tried[r][c]) {
                lastShot = r to c
                return r to c
            }
        }
        // Если вышли из цикла — просто найдём первую свободную клетку "в лоб"
        for (r in 0 until 10) {
            for (c in 0 until 10) {
                if (!tried[r][c]) {
                    lastShot = r to c
                    return r to c
                }
            }
        }
        throw IllegalStateException("No available cells to shoot")
    }

    /**
     * После того, как игрок или компьютер сделали выстрел, вызываем этот метод,
     * чтобы пометить клетку как "обстрелянную" и дать наследнику (стратегии)
     * возможность обновить внутреннее состояние.
     *
     * @param hit  – true, если попали в корабль.
     * @param sunk – true, если потопили корабль целиком.
     */
    final override fun setShotResult(hit: Boolean, sunk: Boolean) {
        lastShot?.let { (r, c) ->
            if (isValidCell(r, c)) {
                tried[r][c] = true
            }
        }
        onShotResult(lastShot, hit, sunk)
    }

    /** Проверяет, что координаты (row, col) лежат в диапазоне 0..9. */
    protected fun isValidCell(row: Int, col: Int): Boolean {
        return row in 0..9 && col in 0..9
    }

    /**
     * «Сырая» логика выбора следующей клетки (row, col), без учёта tried[].
     * Должна возвращать любую пару (row, col) (в идеале – валидную, но проверка будет в getNextShot).
     */
    protected abstract fun computeNextShot(): Pair<Int, Int>

    /**
     * Обработка результата последнего выстрела для обновления внутреннего состояния.
     * @param lastShot – координаты (row, col) последнего выстрела (или null, если ещё не было выстрела)
     * @param hit      – true, если попали в палубу
     * @param sunk     – true, если этот ход потопил корабль целиком
     */
    protected open fun onShotResult(
        lastShot: Pair<Int, Int>?,
        hit: Boolean,
        sunk: Boolean
    ) {
        // По умолчанию – ничего не делаем; наследники переопределяют
    }
}