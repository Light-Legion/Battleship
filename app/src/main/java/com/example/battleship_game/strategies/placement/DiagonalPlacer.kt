package com.example.battleship_game.strategies.placement

import kotlin.random.Random

class DiagonalPlacer(rand: Random = Random.Default)
    : BasePlacementStrategy(rand) {

    override fun scanCells(): List<Pair<Int, Int>> {
        val out = mutableListOf<Pair<Int,Int>>()
        for (r in 0..9) {
            for (c in 0..9) {
                // пропускаем клетки на главной и побочной диагонали
                if (r == c || r + c == 9) continue
                out += r to c
            }
        }
        return out.shuffled(rand)
    }

    // Переопределяем canPlace, чтобы запретить палубы на диагоналях
    override fun canPlace(
        occ: Array<BooleanArray>,
        x0: Int, y0: Int,
        size: Int,
        horizontal: Boolean
    ): Boolean {
        // сначала проверяем базовые границы
        if (!super.canPlace(occ, x0, y0, size, horizontal)) return false

        // теперь проверяем, что ни одна палуба не попадёт на r==c или r+c==9
        val dx = if (horizontal) 1 else 0
        val dy = if (horizontal) 0 else 1
        for (k in 0 until size) {
            val x = x0 + dx * k
            val y = y0 + dy * k

            if (x == y || x + y == 9) {
                return false
            }
        }
        return true
    }
}