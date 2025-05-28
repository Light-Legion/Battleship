package com.example.battleship_game.strategies.placement

import com.example.battleship_game.data.model.ShipPlacement

class CoastsPlacer : PlacementStrategy {
    override fun generatePlacement(): List<ShipPlacement> {
        val sizes = listOf(4,3,3,2,2,2,1,1,1,1)
        val placements = mutableListOf<ShipPlacement>()
        val occupied = Array(10){ BooleanArray(10){false} }
        val rand = java.util.Random()

        fun fitsAndMark(x: Int, y: Int, size: Int, horizontal: Boolean): Boolean {
            // Проверяем границы и отсутствие пересечений
            for (i in 0 until size) {
                val cx = if (horizontal) x + i else x
                val cy = if (horizontal) y else y + i
                if (cy !in 0..9 || cx !in 0..9 || occupied[cy][cx]) return false
            }
            // Убеждаемся, что корабль целиком на границе
            if (horizontal) {
                if (y != 0 && y != 9) return false
            } else {
                if (x != 0 && x != 9) return false
            }
            // Помечаем занятость
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
                        // Горизонтально в строке 0 или 9
                        val y = if (rand.nextBoolean()) 0 else 9
                        val x = rand.nextInt(11 - size)
                        if (fitsAndMark(x, y, size, true)) {
                            placements.add(ShipPlacement(x, y, size, true))
                            placed = true; break
                        }
                    } else {
                        // Вертикально в столбце 0 или 9
                        val x = if (rand.nextBoolean()) 0 else 9
                        val y = rand.nextInt(11 - size)
                        if (fitsAndMark(x, y, size, false)) {
                            placements.add(ShipPlacement(x, y, size, false))
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