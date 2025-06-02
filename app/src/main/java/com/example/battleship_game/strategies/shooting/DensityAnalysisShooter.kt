package com.example.battleship_game.strategies.shooting

import kotlin.random.Random

/**
 * Стратегия "Анализ плотности":
 * 1. Рассчитывает вероятности нахождения кораблей
 * 2. Выбирает клетку с максимальной вероятностью
 * 3. Реализует добивание как в RandomFinishingShooter
 *
 * Особенности:
 * - Кэширование расчетов вероятностей
 * - Учет оставшихся кораблей
 * - Эвристика для определения размера потопленного корабля
 */
class DensityAnalysisShooter : BaseShootingStrategy() {
    private val random = Random.Default
    private val targetQueue = ArrayDeque<Pair<Int, Int>>()
    private var firstHit: Pair<Int, Int>? = null
    private var secondHit: Pair<Int, Int>? = null
    private val remainingShips = mutableListOf(4, 3, 3, 2, 2, 2, 1, 1, 1, 1)
    private var probabilityCache: Array<IntArray>? = null
    private var needsRecalculation = true

    override fun computeNextShot(): Pair<Int, Int> {
        // 1. Проверка очереди добивания
        if (targetQueue.isNotEmpty()) {
            return targetQueue.removeFirst()
        }

        // 2. Пересчет вероятностей при необходимости
        if (needsRecalculation) {
            recalculateProbability()
            needsRecalculation = false
        }

        // 3. Поиск клетки с максимальной вероятностью
        val probability = probabilityCache ?: Array(10) { IntArray(10) }
        var maxProb = -1
        var bestCell = 0 to 0

        for (y in 0..9) {
            for (x in 0..9) {
                if (!tried[y][x] && probability[y][x] > maxProb) {
                    maxProb = probability[y][x]
                    bestCell = x to y
                }
            }
        }

        return if (maxProb > 0) bestCell else getRandomCell()
    }

    override fun onShotResult(lastShot: Pair<Int, Int>?, hit: Boolean, sunk: Boolean) {
        // Флаг для пересчета вероятностей
        needsRecalculation = true

        if (lastShot == null) return
        val (x, y) = lastShot

        when {
            // Первое попадание в корабль
            hit && !sunk && firstHit == null -> {
                firstHit = x to y
                addOrthogonalNeighbors(x, y)
            }

            // Второе попадание (определение направления)
            hit && !sunk && secondHit == null -> {
                secondHit = x to y
                determineDirection()
            }

            // Корабль потоплен
            sunk -> handleSunkShip()
        }
    }

    /** Пересчитывает вероятности расположения кораблей */
    private fun recalculateProbability() {
        val probability = Array(10) { IntArray(10) }
        val shipGroups = remainingShips.groupBy { it }

        shipGroups.forEach { (size, ships) ->
            val count = ships.size

            // Горизонтальные размещения
            for (y in 0..9) {
                for (x in 0..(10 - size)) {
                    if (canPlaceHorizontally(x, y, size)) {
                        for (i in 0 until size) {
                            probability[y][x + i] += count
                        }
                    }
                }
            }

            // Вертикальные размещения
            for (x in 0..9) {
                for (y in 0..(10 - size)) {
                    if (canPlaceVertically(x, y, size)) {
                        for (i in 0 until size) {
                            probability[y + i][x] += count
                        }
                    }
                }
            }
        }

        probabilityCache = probability
    }

    /** Проверяет возможность горизонтального размещения */
    private fun canPlaceHorizontally(x: Int, y: Int, size: Int): Boolean {
        for (i in 0 until size) {
            if (tried[y][x + i]) return false
        }
        return true
    }

    /** Проверяет возможность вертикального размещения */
    private fun canPlaceVertically(x: Int, y: Int, size: Int): Boolean {
        for (i in 0 until size) {
            if (tried[y + i][x]) return false
        }
        return true
    }

    /** Обрабатывает потопление корабля */
    private fun handleSunkShip() {
        // Эвристика: удаляем самый большой оставшийся корабль
        remainingShips.maxOrNull()?.let { size ->
            remainingShips.remove(size)
        }

        resetHuntingState()
    }

    /** Добавляет 4 ортогональных соседа */
    private fun addOrthogonalNeighbors(x: Int, y: Int) {
        listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1).forEach { (dx, dy) ->
            val nx = x + dx
            val ny = y + dy
            if (isValidCell(nx, ny) && !tried[ny][nx]) {
                targetQueue.addLast(nx to ny)
            }
        }
    }

    /** Определяет направление корабля */
    private fun determineDirection() {
        val (firstX, firstY) = firstHit ?: return
        val (secondX, secondY) = secondHit ?: return

        val dx = (secondX - firstX).coerceIn(-1, 1)
        val dy = (secondY - firstY).coerceIn(-1, 1)

        targetQueue.clear()
        addInDirection(secondX, secondY, dx, dy)
        addInDirection(firstX, firstY, -dx, -dy)
    }

    /** Добавляет клетки в направлении */
    private fun addInDirection(startX: Int, startY: Int, dx: Int, dy: Int) {
        var x = startX + dx
        var y = startY + dy

        while (isValidCell(x, y) && !tried[y][x]) {
            targetQueue.addLast(x to y)
            x += dx
            y += dy
        }
    }

    /** Сбрасывает состояние охоты */
    private fun resetHuntingState() {
        firstHit = null
        secondHit = null
        targetQueue.clear()
    }

    /** Выбирает случайную необстрелянную клетку */
    private fun getRandomCell(): Pair<Int, Int> {
        val available = mutableListOf<Pair<Int, Int>>()
        for (y in 0..9) for (x in 0..9) {
            if (!tried[y][x]) available.add(x to y)
        }
        return available.random(random)
    }
}
