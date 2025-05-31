package com.example.battleship_game.strategies.placement

import com.example.battleship_game.data.model.ShipPlacement
import kotlin.random.Random

abstract class BasePlacementStrategy(
    protected val rand: Random = Random.Default
) : PlacementStrategy {

    /** Флот: (длина → уникальный shipId) */
    private val fleet = listOf(
        4 to 1,
        3 to 2, 3 to 3,
        2 to 4, 2 to 5, 2 to 6,
        1 to 7, 1 to 8, 1 to 9, 1 to 10
    )

    protected fun getFleet() = fleet

    override fun generatePlacement(): List<ShipPlacement> {
        val maxAttempts = 1000
        repeat(maxAttempts) {
            // 1) Очистить поле и очередь
            val occ = Array(10) { BooleanArray(10) }
            val queue = fleet.shuffled(rand).toMutableList()
            val result = mutableListOf<ShipPlacement>()
            val cells = scanCells()

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

    protected open fun canPlace(
        occ: Array<BooleanArray>,
        x0: Int, y0: Int,
        size: Int,
        horizontal: Boolean
    ): Boolean {
        val dx = if (horizontal) 1 else 0
        val dy = if (horizontal) 0 else 1
        val endX = x0 + dx * (size - 1)
        val endY = y0 + dy * (size - 1)
        // 1) выход за границы?
        if (endX !in 0..9 || endY !in 0..9) return false

        // 2) для каждой палубы проверяем саму её и 8 соседей
        for (k in 0 until size) {
            val x = x0 + dx * k
            val y = y0 + dy * k

            for (ry in (y - 1)..(y + 1)) {
                for (rx in (x - 1)..(x + 1)) {
                    if (rx in 0..9 && ry in 0..9) {
                        if (occ[ry][rx]) {
                            return false
                        }
                    }
                }
            }
        }
        return true
    }

    protected open fun tryPlace(
        occ: Array<BooleanArray>,
        x0: Int, y0: Int,
        size: Int,
        horizontal: Boolean
    ): Boolean {
        // сначала проверяем, можно ли поставить
        if (!canPlace(occ, x0, y0, size, horizontal)) return false

        val dx = if (horizontal) 1 else 0
        val dy = if (horizontal) 0 else 1

        // 3) помечаем **только** палубы
        for (k in 0 until size) {
            val x = x0 + dx * k
            val y = y0 + dy * k
            occ[y][x] = true
        }

        return true
    }


    /**
     * Каждая стратегия выдаёт список (row,col) в том порядке,
     * в котором мы будем пытаться там разместить корабли.
     */
    protected abstract fun scanCells(): List<Pair<Int, Int>>
}
