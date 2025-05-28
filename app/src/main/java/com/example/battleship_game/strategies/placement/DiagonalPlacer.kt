package com.example.battleship_game.strategies.placement

import com.example.battleship_game.data.model.ShipPlacement

class DiagonalPlacer : PlacementStrategy {
    override fun generatePlacement(): List<ShipPlacement> {
        val sizes = listOf(4,3,3,2,2,2,1,1,1,1)
        val placements = mutableListOf<ShipPlacement>()
        val occupied = Array(10){ BooleanArray(10){false} }
        val rand = java.util.Random()

        fun fitsAndMark(x: Int, y: Int, size: Int, horizontal: Boolean): Boolean {
            for (i in 0 until size) {
                val cx = if (horizontal) x + i else x
                val cy = if (horizontal) y else y + i
                // Все клетки должны быть свободны и в пределах
                if (cy !in 0..9 || cx !in 0..9 || occupied[cy][cx]) return false
                // Требуем, чтобы начальная клетка была на диагонали
                if (i == 0 && cx != cy) return false
            }
            for (i in 0 until size) {
                val cx = if (horizontal) x + i else x
                val cy = if (horizontal) y else y + i
                occupied[cy][cx] = true
            }
            return true
        }

        placementLoop@ while (placements.size < sizes.size) {
            placements.clear()
            for (row in occupied) for (i in row.indices) row[i] = false
            for (size in sizes) {
                var placed = false
                for (attempt in 1..100) {
                    val horizontal = rand.nextBoolean()
                    val i = rand.nextInt(11 - size)
                    if (horizontal) {
                        // Начинаем горизонтальный корабль с (i,i)
                        if (fitsAndMark(i, i, size, true)) {
                            placements.add(ShipPlacement(i, i, size, true))
                            placed = true; break
                        }
                    } else {
                        // Начинаем вертикальный корабль с (i,i)
                        if (fitsAndMark(i, i, size, false)) {
                            placements.add(ShipPlacement(i, i, size, false))
                            placed = true; break
                        }
                    }
                }
                if (!placed) continue@placementLoop
            }
        }
        return placements
    }
}