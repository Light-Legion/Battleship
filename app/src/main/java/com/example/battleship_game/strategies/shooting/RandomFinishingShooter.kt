package com.example.battleship_game.strategies.shooting

import kotlin.random.Random

/**
 * Стратегия "Случайный выстрел с добиванием":
 * 1. Приоритет - очередь добивания (если не пуста)
 * 2. Иначе - случайная необстрелянная клетка
 *
 * При попадании:
 * - Первое попадание: добавляет 4 соседние клетки
 * - Второе попадание: определяет направление и добавляет клетки вдоль линии
 * - Потопление: сбрасывает состояние добивания
 */
class RandomFinishingShooter : BaseShootingStrategy() {
    private val random = Random.Default
    private val targetQueue = ArrayDeque<Pair<Int, Int>>()
    private var firstHit: Pair<Int, Int>? = null
    private var secondHit: Pair<Int, Int>? = null

    override fun computeNextShot(): Pair<Int, Int> {
        // 1. Проверка очереди добивания
        while (targetQueue.isNotEmpty()) {
            val cell = targetQueue.removeFirst()
            if (isValidCell(cell.first, cell.second) && !tried[cell.second][cell.first]) {
                return cell
            }
        }

        // 2. Случайная необстрелянная клетка
        val availableCells = mutableListOf<Pair<Int, Int>>()
        for (y in 0..9) {
            for (x in 0..9) {
                if (!tried[y][x]) availableCells.add(x to y)
            }
        }
        return availableCells.random(random)
    }

    override fun onShotResult(lastShot: Pair<Int, Int>?, hit: Boolean, sunk: Boolean) {
        if (lastShot == null) return
        val (x, y) = lastShot

        when {
            // Первое попадание в корабль
            hit && !sunk && firstHit == null -> {
                firstHit = x to y
                addOrthogonalNeighbors(x, y)
            }

            // Второе попадание (определяем направление)
            hit && !sunk && secondHit == null -> {
                secondHit = x to y
                determineDirection()
            }

            // Корабль потоплен
            sunk -> resetHuntingState()
        }
    }

    /** Добавляет 4 ортогональных соседа */
    private fun addOrthogonalNeighbors(x: Int, y: Int) {
        val neighbors = listOf(
            x + 1 to y,  // право
            x - 1 to y,  // лево
            x to y + 1,  // низ
            x to y - 1   // верх
        )

        neighbors.forEach { (nx, ny) ->
            if (isValidCell(nx, ny) && !tried[ny][nx]) {
                targetQueue.addLast(nx to ny)
            }
        }
    }

    /** Определяет направление корабля по двум попаданиям */
    private fun determineDirection() {
        val (firstX, firstY) = firstHit ?: return
        val (secondX, secondY) = secondHit ?: return

        val dx = (secondX - firstX).coerceIn(-1, 1)
        val dy = (secondY - firstY).coerceIn(-1, 1)

        targetQueue.clear()

        // Добавляем клетки в направлении попадания
        addInDirection(secondX, secondY, dx, dy)
        addInDirection(firstX, firstY, -dx, -dy)
    }

    /** Добавляет клетки в заданном направлении */
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
}