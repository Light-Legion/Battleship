package com.example.battleship_game.strategies.placement

import com.example.battleship_game.data.model.ShipPlacement
import java.util.Random

/**
 * Расставляет корабли на границах поля (строки 0 или 9 для горизонтали,
 * столбцы 0 или 9 для вертикали).
 */
class CoastsPlacer : PlacementStrategy {

    override fun generatePlacement(): List<ShipPlacement> {
        val sizes = listOf(4,3,3,2,2,2,1,1,1,1)
        val occupied = Array(10) { BooleanArray(10) }
        val rand = Random()

        placement@ while (true) {
            val placements = mutableListOf<ShipPlacement>()
            // Сбросим занятость
            for (row in occupied) row.fill(false)

            for (i in sizes.indices) {
                val size = sizes[i]
                val shipId = i + 1
                var placed = false

                // До 100 попыток на каждый корабль
                for (attempt in 1..100) {
                    val horizontal = rand.nextBoolean()
                    val x: Int
                    val y: Int

                    if (horizontal) {
                        // Горизонтально: строка 0 или 9
                        y = if (rand.nextBoolean()) 0 else 9
                        x = rand.nextInt(11 - size)
                    } else {
                        // Вертикально: столбец 0 или 9
                        x = if (rand.nextBoolean()) 0 else 9
                        y = rand.nextInt(11 - size)
                    }

                    // Проверка границ и занятости
                    var ok = true
                    for (k in 0 until size) {
                        val cx = if (horizontal) x + k else x
                        val cy = if (horizontal) y else y + k
                        if (cx !in 0..9 || cy !in 0..9 || occupied[cy][cx]) {
                            ok = false; break
                        }
                    }
                    if (!ok) continue

                    // Помечаем занятость
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

                if (!placed) {
                    // Не удалось поставить этот корабль — начинаем сначала
                    continue@placement
                }
            }

            // Если дошли до конца — все 10 расставлено успешно
            return placements
        }
    }
}