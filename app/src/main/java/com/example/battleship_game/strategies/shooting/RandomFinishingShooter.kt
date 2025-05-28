package com.example.battleship_game.strategies.shooting

import java.util.Random

/**
 * Базовая стратегия: случайный выбор до попадания,
 * после попадания переходим в режим «добивания» —
 * добавляем в очередь соседние клетки.
 */
class RandomFinishingShooter : ShootingStrategy {
    private val rand = Random()
    private val tried = Array(10) { BooleanArray(10) }
    private val targetQueue = ArrayDeque<Pair<Int, Int>>()
    private var lastShot: Pair<Int, Int>? = null

    override fun getNextShot(): Pair<Int, Int> {
        // 1) Если в очереди «добивания» есть валидные клетки — стреляем по ним
        while (targetQueue.isNotEmpty()) {
            val (x, y) = targetQueue.removeFirst()
            if (x in 0..9 && y in 0..9 && !tried[y][x]) {
                lastShot = x to y
                return lastShot!!
            }
        }
        // 2) Иначе — случайная незанятая клетка
        var x: Int
        var y: Int
        do {
            x = rand.nextInt(10)
            y = rand.nextInt(10)
        } while (tried[y][x])
        lastShot = x to y
        return lastShot!!
    }

    override fun setShotResult(hit: Boolean, sunk: Boolean) {
        val (x, y) = lastShot ?: return
        tried[y][x] = true

        if (hit && !sunk) {
            // Добавляем в очередь соседние клетки
            targetQueue.add(x + 1 to y)
            targetQueue.add(x - 1 to y)
            targetQueue.add(x to y + 1)
            targetQueue.add(x to y - 1)
        }

        if (hit && sunk) {
            // Потопленный — сбрасываем очередь «добивания»
            targetQueue.clear()
        }
    }
}