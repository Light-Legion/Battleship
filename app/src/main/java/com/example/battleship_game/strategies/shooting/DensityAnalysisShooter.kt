package com.example.battleship_game.strategies.shooting

/**
 * Вероятностная стратегия (анализ плотности).
 * Для каждого оставшегося корабля (по длинам в remainingShips)
 * рассчитывает, сколько раз каждая клетка участвует в возможных
 * размещениях — выбираем клетку с наибольшей «вероятностью».
 */
class DensityAnalysisShooter : ShootingStrategy {
    private val tried = Array(10) { BooleanArray(10) }
    private val remainingShips = mutableListOf(4, 3, 3, 2, 2, 2, 1, 1, 1, 1)
    private var lastShot: Pair<Int, Int>? = null

    override fun getNextShot(): Pair<Int, Int> {
        // 1) Строим карту вероятностей
        val probability = Array(10) { IntArray(10) }

        for (size in remainingShips) {
            // горизонтальные
            for (y in 0..9) {
                for (x in 0..10 - size) {
                    if ((0 until size).all { !tried[y][x + it] }) {
                        (0 until size).forEach { i -> probability[y][x + i]++ }
                    }
                }
            }
            // вертикальные
            for (x in 0..9) {
                for (y in 0..10 - size) {
                    if ((0 until size).all { !tried[y + it][x] }) {
                        (0 until size).forEach { i -> probability[y + i][x]++ }
                    }
                }
            }
        }

        // 2) Выбираем максимальную вероятность
        var best = 0 to 0
        var maxProb = -1
        for (y in 0..9) for (x in 0..9) {
            if (!tried[y][x] && probability[y][x] > maxProb) {
                maxProb = probability[y][x]
                best = x to y
            }
        }

        lastShot = best
        return best
    }

    override fun setShotResult(hit: Boolean, sunk: Boolean) {
        val (x, y) = lastShot ?: return
        tried[y][x] = true

        if (hit && sunk) {
            // Эвристическое удаление:
            // считаем, что количество подряд попаданий до sunk == размер корабля
            // TODO: заменить на точное удаление размера при доступе к реальной длине потопленного корабля
            // Здесь мы делаем простую эвристику: убираем из remainingShips
            // наибольший элемент ≤ количества непрерывных попаданий (приближенно).
            val sunkSizeGuess = remainingShips.maxOrNull() ?: return
            remainingShips.remove(sunkSizeGuess)
        }
    }
}