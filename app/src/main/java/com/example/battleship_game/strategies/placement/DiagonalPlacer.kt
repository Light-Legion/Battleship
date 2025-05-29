// DiagonalScanStrategy.kt
package com.example.battleship_game.strategies.placement

import kotlin.random.Random

// 3) По всем диагоналям
class DiagonalPlacer(rand: Random = Random.Default)
    : BasePlacementStrategy(rand) {

    override fun scanCells(): List<Pair<Int,Int>> {
        val out = mutableListOf<Pair<Int,Int>>()
        val n = 10

        // ↘ диагонали (r-c = d)
        for (d in -(n-1)..(n-1)) {
            for (r in 0 until n) {
                val c = r - d
                if (c in 0 until n) out += r to c
            }
        }
        // ↙ диагонали (r+c = s)
        for (s in 0..2*(n-1)) {
            for (r in 0 until n) {
                val c = s - r
                if (c in 0 until n) out += r to c
            }
        }
        return out
    }
}
