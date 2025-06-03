package com.example.battleship_game.strategies.shooting

/**
 * Базовая абстракция для любой стратегии стрельбы.
 *
 * Основная ответственность:
 *  - Хранить матрицу tried[row][col]: true – если клетка уже обстреляна.
 *  - Проверять валидность координат (0..9).
 *  - Обеспечивать контракт getNextShot() → computeNextShot() → setShotResult().
 *
 * Наследники переопределяют:
 *  - computeNextShot()  – возвращает (row, col) для следующего выстрела, без учёта tried[].
 *  - onShotResult(...)  – получает результат (hit/sunk) последнего выстрела и обновляет внутреннее состояние.
 */
abstract class BaseShootingStrategy : ShootingStrategy {
    // tried[row][col] = true, если эта клетка уже обстреляна
    private val tried = Array(10) { BooleanArray(10) { false } }

    // Последний выбранный выстрел (row, col)
    private var lastShot: Pair<Int, Int>? = null

    /**
     * Финальный метод, возвращающий следующую клетку для выстрела.
     * Гарантирует:
     *  1) невалидные координаты от computeNextShot() будут отфильтрованы,
     *  2) не будет выбрана уже "tried" клетка,
     *  3) если стратегия продолжает выдавать плохие координаты, после 100 попыток избирается
     *     первая свободная клетка в последовательном обходе.
     */
    final override fun getNextShot(): Pair<Int, Int> {
        repeat(100) {
            val (r, c) = computeNextShot()
            if (isValidCell(r, c) && !tried[r][c]) {
                lastShot = r to c
                return r to c
            }
        }
        // После 100 неудачных попыток вернуть первую свободную клетку “в лоб”
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
     * После того, как игрок или компьютер сделали выстрел, вызывается этот метод
     * для пометки tried[row][col] = true и передачи результата стратегии.
     *
     * @param hit  – true, если попадание.
     * @param sunk – true, если этот выстрел потопил корабль целиком.
     */
    final override fun setShotResult(hit: Boolean, sunk: Boolean) {
        lastShot?.let { (r, c) ->
            if (isValidCell(r, c)) {
                tried[r][c] = true
            }
        }
        onShotResult(lastShot, hit, sunk)
    }

    /**
     * Проверяет, что координаты (row, col) лежат в диапазоне 0..9.
     */
    protected fun isValidCell(row: Int, col: Int): Boolean {
        return row in 0..9 && col in 0..9
    }

    /**
     * «Сырая» логика выбора следующей клетки (row, col), без учёта tried[].
     * Наследник должен реализовать этот метод.
     * Возвращаемый пар (row, col) затем проверяется в getNextShot().
     */
    protected abstract fun computeNextShot(): Pair<Int, Int>

    /**
     * Обработка результата последнего выстрела для обновления внутреннего состояния.
     * Наследник может переопределить.
     *
     * @param lastShot – координаты (row, col) последнего выстрела (или null, если ещё не было выстрела).
     * @param hit      – true, если попал в корабль.
     * @param sunk     – true, если этот ход потопил корабль целиком.
     */
    protected open fun onShotResult(
        lastShot: Pair<Int, Int>?,
        hit: Boolean,
        sunk: Boolean
    ) {
        // по умолчанию не делаем ничего
    }

    // Внутри BaseShootingStrategy:
    protected fun hasTried(row: Int, col: Int): Boolean {
        return tried[row][col]
    }

}
