// HalfFieldPlacer.kt
package com.example.battleship_game.strategies.placement

import kotlin.random.Random

class HalfFieldPlacer(rand: Random = Random.Default)
    : BasePlacementStrategy(rand) {

    // при создании один раз определяем, какую половину используем
    private val useLeft = rand.nextBoolean()

    override fun scanCells(): List<Pair<Int, Int>> {
        val minCol = if (useLeft) 0 else 5
        val maxCol = if (useLeft) 4 else 9
        val out = mutableListOf<Pair<Int, Int>>()
        // сначала первая половина (столбцы minCol..maxCol)
        for (r in 0..9) for (c in minCol..maxCol) {
            out += r to c
        }
        // если что-то осталось, докидываем вторую половину
        val otherMin = if (useLeft) 5 else 0
        val otherMax = if (useLeft) 9 else 4
        for (r in 0..9) for (c in otherMin..otherMax) {
            out += r to c
        }
        return out
    }
}
