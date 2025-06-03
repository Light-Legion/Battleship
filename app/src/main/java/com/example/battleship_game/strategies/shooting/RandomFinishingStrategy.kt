package com.example.battleship_game.strategies.shooting

import kotlin.random.Random

/**
 * Стратегия "Случайный выстрел с добиванием" (уровень EASY–MEDIUM):
 * 1) Если есть «очередь добивания» (targetQueue) – брать первый элемент.
 * 2) Иначе – выбирать случайную неиспользованную клетку.
 *
 * После попадания:
 *  - Если было первое попадание → сохранить (row, col) как firstHit и в очередь добавить
 *    4 ортогональных соседа.
 *  - Если было второе попадание → определить方向, заполнить очередь «вдоль линии».
 *  - Если было потопление → очистить «режим добивания».
 */
class RandomFinishingStrategy : BaseShootingStrategy() {
    private val random = Random.Default

    // Очередь координат (row, col) для режима «добивания»
    private val targetQueue = ArrayDeque<Pair<Int, Int>>()

    // Первое и второе попадания подряд (для определения “линии”)
    private var firstHit: Pair<Int, Int>?  = null
    private var secondHit: Pair<Int, Int>? = null

    override fun computeNextShot(): Pair<Int, Int> {
        // 1) Если очередь добивания непуста, попытаться взять оттуда первую валидную клетку:
        while (targetQueue.isNotEmpty()) {
            val (r, c) = targetQueue.removeFirst()
            if (isValidCell(r, c) && !tried[r][c]) {
                return r to c
            }
        }

        // 2) Иначе – собрать все неиспользованные (row, col) и взять случайную:
        val available = mutableListOf<Pair<Int, Int>>()
        for (r in 0..9) {
            for (c in 0..9) {
                if (!tried[r][c]) {
                    available.add(r to c)
                }
            }
        }
        // На всякий случай – если available пуст (теоретически не должно быть) – бросить
        if (available.isEmpty()) {
            for (r in 0..9) {
                for (c in 0..9) {
                    if (!tried[r][c]) {
                        return r to c
                    }
                }
            }
        }
        return available.random(random)
    }

    override fun onShotResult(lastShot: Pair<Int, Int>?, hit: Boolean, sunk: Boolean) {
        if (lastShot == null) return
        val (r, c) = lastShot

        when {
            // 1) Попал и **не** потопил, и это самое первое попадание:
            hit && !sunk && firstHit == null -> {
                firstHit = r to c
                addOrthogonalNeighbors(r, c)
            }

            // 2) Попал и **не** потопил, но у нас уже было одно попадание → это второе:
            hit && !sunk && secondHit == null -> {
                secondHit = r to c
                determineDirection()
            }

            // 3) Потопил → сбросить «режим добивания»
            hit && sunk -> {
                resetHuntingState()
            }
        }
    }

    /** Добавляет в конец очереди точки (орты) вокруг (r, c), если они валидны. */
    private fun addOrthogonalNeighbors(row: Int, col: Int) {
        val neighbors = listOf(
            row     to (col + 1),
            row     to (col - 1),
            (row+1) to col,
            (row-1) to col
        )
        neighbors.forEach { (r, c) ->
            if (isValidCell(r, c) && !tried[r][c]) {
                targetQueue.addLast(r to c)
            }
        }
    }

    /**
     * Когда есть два попадания (firstHit и secondHit), определяем направление корабля
     * и наполняем очередь точками вдоль этой линии.
     */
    private fun determineDirection() {
        val (r1, c1) = firstHit ?: return
        val (r2, c2) = secondHit ?: return

        // Вычисляем шаг dx, dy = −1/0/+1
        val dr = (r2 - r1).coerceIn(-1, 1)
        val dc = (c2 - c1).coerceIn(-1, 1)

        // Очищаем очередь, т.к. теперь хотим стрелять строго вдоль этой линии
        targetQueue.clear()

        // От второго попадания идём в направлении (dr, dc)
        var rr = r2 + dr
        var cc = c2 + dc
        while (isValidCell(rr, cc) && !tried[rr][cc]) {
            targetQueue.addLast(rr to cc)
            rr += dr; cc += dc
        }

        // От первого попадания идём в направлении (-dr, -dc)
        rr = r1 - dr
        cc = c1 - dc
        while (isValidCell(rr, cc) && !tried[rr][cc]) {
            targetQueue.addLast(rr to cc)
            rr -= dr; cc -= dc
        }
    }

    /** Сброс всех “промежуточных” переменных, связанных с hunt‐mode */
    private fun resetHuntingState() {
        firstHit  = null
        secondHit = null
        targetQueue.clear()
    }
}