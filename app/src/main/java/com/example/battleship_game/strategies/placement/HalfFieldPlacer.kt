package com.example.battleship_game.strategies.placement

import com.example.battleship_game.data.model.ShipPlacement
import kotlin.random.Random

class HalfFieldPlacer(rand: Random = Random.Default)
    : BasePlacementStrategy(rand) {

    // при создании один раз определяем, какую половину используем
    private val useLeft = rand.nextBoolean()
    private val primaryCols = if (useLeft) 0..4 else 5..9

    override fun generatePlacement(): List<ShipPlacement> {
        // Сначала пытаемся разместить только в основной половине
        val primaryResult = tryGeneratePlacement(true)
        if (primaryResult.isNotEmpty()) return primaryResult

        // Если не получилось - используем всё поле
        return tryGeneratePlacement(false)
    }

    // r - это row, c - это column
    override fun scanCells(): List<Pair<Int, Int>> {
        // сначала _только_ выбранная половина
        val out = mutableListOf<Pair<Int, Int>>()

        for (r in 0..9) {
            for (c in primaryCols) {
                out += r to c
            }
        }

        return out
    }

    private fun scanCellsAll(): List<Pair<Int, Int>> {
        // сначала _только_ выбранная половина
        val out = mutableListOf<Pair<Int, Int>>()

        for (r in 0..9) {
            for (c in primaryCols) {
                out += r to c
            }
        }

        if (useLeft) {
            // Для левой основной: правая резервная (x = 5 to 9)
            for (r in 9 downTo 0) {
                for (c in 5..9) {
                    out += r to c
                }
            }
        } else {
            // Для правой основной: левая резервная (x = 4 downTo 0)
            for (r in 9 downTo 0) {
                for (c in 4 downTo 0) {
                    out += r to c
                }
            }
        }

        return out
    }

    private fun tryGeneratePlacement(full : Boolean): List<ShipPlacement> {
        val maxAttempts = 1000
        repeat(maxAttempts) {
            // 1) Очистить поле и очередь
            val occ = Array(10) { BooleanArray(10) }
            val queue = getFleet().shuffled(rand).toMutableList()
            val result = mutableListOf<ShipPlacement>()
            val cells = if (full) scanCells() else scanCellsAll()

            // 2) Пройти по всем клеткам «своей» стратегии
            for ((r, c) in cells) {
                if (queue.isEmpty()) break

                // цикл по очереди кораблей, но не больше её размера
                var tries = 0
                while (tries < queue.size) {
                    val (size, shipId) = queue.removeAt(0)

                    val orientations = listOf(true, false).shuffled(rand)
                    var placed = false

                    for (horizontal in orientations) {
                        if (tryPlace(occ, c, r, size, horizontal)) {
                            result += ShipPlacement(shipId, size, r, c, isVertical = !horizontal)
                            placed = true
                            break
                        }
                    }

                    if (placed) break

                    // не смогли поставить — возвращаем в очередь и пробуем следующий
                    queue += size to shipId
                    tries++
                }
            }

            // 3) Если всё поставили — возвращаем
            if (queue.isEmpty()) return result
        }

        // 4) Если не удалось за все попытки — пусто
        return emptyList()
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

        val dx = if (horizontal) 1 else 0
        val endX = x0 + dx * (size - 1)
        // 1) выход за границы?
        if (useLeft) {
            if (endX !in 0..4) return false
        }

        return true
    }
}