package com.example.battleship_game.strategies.placement

import com.example.battleship_game.data.model.ShipPlacement

class HalfFieldPlacer : PlacementStrategy {
    override fun generatePlacement(): List<ShipPlacement> {
        val sizes = listOf(4,3,3,2,2,2,1,1,1,1)
        val placements = mutableListOf<ShipPlacement>()
        val occupied = Array(10){ BooleanArray(10){false} }
        val rand = java.util.Random()

        fun fitsAndMark(x: Int, y: Int, size: Int, horizontal: Boolean): Boolean {
            // Проверяем, чтобы корабль не выходил за левую половину (столбцы 0..4)
            if (horizontal) {
                if (x + size - 1 > 4) return false
            } else {
                if (x > 4) return false
            }
            // Проверяем свободные клетки
            for (i in 0 until size) {
                val cx = if (horizontal) x + i else x
                val cy = if (horizontal) y else y + i
                if (cy !in 0..9 || cx !in 0..9 || occupied[cy][cx]) return false
            }
            // Отмечаем занятость
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
                    if (horizontal) {
                        val row = rand.nextInt(10)
                        val col = rand.nextInt(5)  // левая часть
                        if (fitsAndMark(col, row, size, true)) {
                            placements.add(ShipPlacement(col, row, size, true))
                            placed = true; break
                        }
                    } else {
                        val col = rand.nextInt(5)
                        val row = rand.nextInt(11 - size)
                        if (fitsAndMark(col, row, size, false)) {
                            placements.add(ShipPlacement(col, row, size, false))
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