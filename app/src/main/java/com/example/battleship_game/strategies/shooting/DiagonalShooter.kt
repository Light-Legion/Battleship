package com.example.battleship_game.strategies.shooting

import java.util.Random

/**
 * Стратегия диагонального (шахматного) сканирования.
 * Сначала обходим «чёрные» клетки шахматного поля, потом — случайные.
 */
class DiagonalShooter : ShootingStrategy {
    private val rand = Random()
    private val tried = Array(10) { BooleanArray(10) }
    private val pattern = mutableListOf<Pair<Int, Int>>()
    private var lastShot: Pair<Int, Int>? = null

    init {
        // Формируем шахматный паттерн
        for (y in 0..9) {
            for (x in 0..9 step 2) {
                pattern.add((x + (y % 2)) to y)
            }
        }
        pattern.shuffle()
    }

    override fun getNextShot(): Pair<Int, Int> {
        // 1) Сканируем по диагонали
        while (pattern.isNotEmpty()) {
            val (x, y) = pattern.removeAt(0)
            if (!tried[y][x]) {
                lastShot = x to y
                return lastShot!!
            }
        }
        // 2) Если паттерн исчерпан — случайная незанятая клетка
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
        lastShot?.let { (x, y) -> tried[y][x] = true }
        // Диагональная стратегия не ведёт «добивку», поэтому больше ничего не делаем
    }
}