package com.example.battleship_game.strategies.placement

import com.example.battleship_game.data.model.ShipPlacement
import java.util.Random

/**
 * Расставляет корабли так, чтобы их начальная клетка лежала на главной диагонали (x==y).
 */
class DiagonalPlacer : PlacementStrategy {

    override fun generatePlacement(): List<ShipPlacement> {
        val sizes = listOf(4,3,3,2,2,2,1,1,1,1)
        val occupied = Array(10) { BooleanArray(10) }
        val rand = Random()

        placement@ while (true) {
            val placements = mutableListOf<ShipPlacement>()
            for (row in occupied) row.fill(false)

            for (i in sizes.indices) {
                val size = sizes[i]
                val shipId = i + 1
                var placed = false

                for (attempt in 1..100) {
                    val horizontal = rand.nextBoolean()
                    val start = rand.nextInt(11 - size) // индекс на диагонали
                    val x = start
                    val y = start

                    // Проверка, что все клетки свободны
                    var ok = true
                    for (k in 0 until size) {
                        val cx = if (horizontal) x + k else x
                        val cy = if (horizontal) y else y + k
                        if (cx !in 0..9 || cy !in 0..9 || occupied[cy][cx]) {
                            ok = false; break
                        }
                    }
                    if (!ok) continue

                    for (k in 0 until size) {
                        val cx = if (horizontal) x + k else x
                        val cy = if (horizontal) y else y + k
                        occupied[cy][cx] = true
                    }

                    placements.add(
                        ShipPlacement(
                            shipId     = shipId,
                            length     = size,
                            startRow   = y,
                            startCol   = x,
                            isVertical = !horizontal
                        )
                    )
                    placed = true
                    break
                }

                if (!placed) continue@placement
            }

            return placements
        }
    }
}