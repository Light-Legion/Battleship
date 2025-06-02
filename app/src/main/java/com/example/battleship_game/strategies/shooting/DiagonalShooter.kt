package com.example.battleship_game.strategies.shooting

import kotlin.random.Random

/**
 * Стратегия "Расширенные диагонали":
 * 1. Фаза 0: Стрельба по главной диагонали (x=y)
 * 2. Фаза 1: Стрельба по побочной диагонали (x+y=9)
 * 3. Фаза 2: Случайные выстрелы
 *
 * При попадании:
 * - Добавляет ортогональных соседей
 * - При втором попадании определяет направление
 */
class DiagonalShooter : BaseShootingStrategy() {
    private val random = Random.Default
    private val mainDiagonal = (0..9).map { it to it }.toMutableList()
    private val secondaryDiagonal = (0..9).map { it to (9 - it) }.toMutableList()
    private var phase = 0 // 0: main, 1: secondary, 2: random
    private val targetQueue = ArrayDeque<Pair<Int, Int>>()
    private var firstHit: Pair<Int, Int>? = null
    private var secondHit: Pair<Int, Int>? = null

    init {
        // Перемешиваем диагонали для разнообразия
        mainDiagonal.shuffle()
        secondaryDiagonal.shuffle()
    }

    override fun computeNextShot(): Pair<Int, Int> {
        // 1. Проверка очереди добивания
        if (targetQueue.isNotEmpty()) {
            return targetQueue.removeFirst()
        }

        // 2. Выбор в зависимости от фазы
        return when (phase) {
            0 -> getFromDiagonal(mainDiagonal) ?: run {
                phase = 1
                computeNextShot()
            }
            1 -> getFromDiagonal(secondaryDiagonal) ?: run {
                phase = 2
                computeNextShot()
            }
            else -> getRandomCell()
        }
    }

    override fun onShotResult(lastShot: Pair<Int, Int>?, hit: Boolean, sunk: Boolean) {
        if (lastShot == null) return
        val (x, y) = lastShot

        when {
            // Первое попадание
            hit && !sunk && firstHit == null -> {
                firstHit = x to y
                addOrthogonalNeighbors(x, y)
            }

            // Второе попадание
            hit && !sunk && secondHit == null -> {
                secondHit = x to y
                determineDirection()
            }

            // Корабль потоплен
            sunk -> resetHuntingState()
        }
    }

    /** Получает следующую клетку из диагонали */
    private fun getFromDiagonal(diagonal: MutableList<Pair<Int, Int>>): Pair<Int, Int>? {
        while (diagonal.isNotEmpty()) {
            val cell = diagonal.removeAt(0)
            if (!tried[cell.second][cell.first]) {
                return cell
            }
        }
        return null
    }

    /** Выбирает случайную необстрелянную клетку */
    private fun getRandomCell(): Pair<Int, Int> {
        val available = mutableListOf<Pair<Int, Int>>()
        for (y in 0..9) for (x in 0..9) {
            if (!tried[y][x]) available.add(x to y)
        }
        return available.random(random)
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
}