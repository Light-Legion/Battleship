package com.example.battleship_game.strategies.placement

import com.example.battleship_game.data.model.ShipPlacement
import java.util.Random

/**
 * Расставляет все корабли в левой половине поля (столбцы 0..4).
 */
class HalfFieldPlacer : PlacementStrategy {

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
                    val x: Int
                    val y: Int

                    if (horizontal) {
                        // Горизонтально: столбцы 0..4
                        y = rand.nextInt(10)
                        x = rand.nextInt(5 - size + 1)  // с учётом длины
                    } else {
                        // Вертикально: столбцы 0..4, строки 0..(10-size)
                        x = rand.nextInt(5)
                        y = rand.nextInt(11 - size)
                    }

                    // Проверка занятости
                    var ok = true
                    for (k in 0 until size) {
                        val cx = if (horizontal) x + k else x
                        val cy = if (horizontal) y else y + k
                        if (cx !in 0..4 || cy !in 0..9 || occupied[cy][cx]) {
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