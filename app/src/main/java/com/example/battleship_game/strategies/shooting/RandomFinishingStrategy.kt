package com.example.battleship_game.strategies.shooting

import kotlin.random.Random

/**
 * «Случайная с добиванием» стратегия:
 *
 * 1) Хранит mutableSet availableCells со всеми «еще не обстрелянными» клетками.
 * 2) computeNextShot():
 *    • Если в targetQueue (очередь добивания) есть координаты – берём первую доступную.
 *    • Иначе – выбираем случайную точку из availableCells.
 *    После выбора сразу удаляем её из availableCells, чтобы больше не возвращать.
 * 3) onShotResult(lastShot, hit, sunk):
 *    • Всегда удаляем lastShot из availableCells (он уже удалён в computeNextShot).
 *    • Если hit && sunk (потопили):
 *         – добавляем (r,c) в hitCells, если его там ещё нет
 *         – вычисляем «буфер» (вокруг всех hitCells)
 *         – удаляем из availableCells все hitCells и их буфер
 *         – сбрасываем режим добивания (hitCells.clear(), targetQueue.clear(), firstHit/secondHit = null)
 *    • Если hit (но не sunk):
 *         – добавляем (r,c) в hitCells
 *         – пересоздаём очередь добивания, опираясь на актуальный список hitCells
 *    • Если miss – больше ничего не делаем (точка уже удалена из availableCells).
 */
class RandomFinishingStrategy : BaseShootingStrategy() {

    private val random = Random.Default

    /** Очередь координат «добивания». */
    private val targetQueue = ArrayDeque<Pair<Int, Int>>()

    /** Список всех подряд попавших точек текущего (ещё не потопленного) корабля. */
    private val hitCells = mutableListOf<Pair<Int, Int>>()

    /**
     * Множество всех точек, которые ещё не стрелялись.
     * Стратегия никогда не выберет точку, которой нет в этом множестве.
     */
    private val availableCells = mutableSetOf<Pair<Int, Int>>().apply {
        for (r in 0 until 10) {
            for (c in 0 until 10) {
                add(r to c)
            }
        }
    }

    override fun computeNextShot(): Pair<Int, Int> {
        // 1) Если есть очередь добивания, пытаемся взять первую доступную точку
        while (targetQueue.isNotEmpty()) {
            val cell = targetQueue.removeFirst()
            if (cell in availableCells) {
                availableCells.remove(cell)
                return cell
            }
        }

        // 2) Иначе – случайная точка из доступных
        if (availableCells.isNotEmpty()) {
            val randomIndex = random.nextInt(availableCells.size)
            val cell = availableCells.elementAt(randomIndex)
            availableCells.remove(cell)
            return cell
        }

        // 3) Чисто на всякий случай – берем первую «не tried» (теоретически недостижимо)
        for (r in 0 until 10) {
            for (c in 0 until 10) {
                if (!hasTried(r, c)) {
                    return r to c
                }
            }
        }
        throw IllegalStateException("No available cells to shoot")
    }

    override fun onShotResult(
        lastShot: Pair<Int, Int>?,
        hit: Boolean,
        sunk: Boolean
    ) {
        if (lastShot == null) return
        val (r, c) = lastShot

        when {
            // ─── Корабль потоплен ───
            hit && sunk -> {
                // 1) Добавляем последнюю точку в hitCells (если там ещё нет)
                if ((r to c) !in hitCells) {
                    hitCells.add(r to c)
                }
                // 2) Вычисляем буфер вокруг всех hitCells
                val buffer = computeBuffer(hitCells)
                // 3) Удаляем из доступных сам корабль (hitCells) и весь буфер
                availableCells.removeAll(hitCells)
                availableCells.removeAll(buffer)
                // 4) Сброс режима «добивания»
                resetHuntMode()
            }

            // ─── Просто попадание, но корабль ещё не потоплен ───
            hit -> {
                // 1) Добавляем точку в hitCells
                hitCells.add(r to c)
                // 2) Пересоздаём очередь добивания по новой логике
                enqueueBasedOnHits()
            }

            // ─── Промах ───
            else -> {
                // Ничего не надо: lastShot уже удалён из availableCells в computeNextShot
            }
        }
    }

    /**
     * Построение очереди добивания после очередного попадания.
     * Если в hitCells только одна точка → enqueue 4 ортогональных соседей.
     * Если >=2 точек и они выстроены в одну строку → enqueue ровно 2 клетки: слева от min_col и справа от max_col.
     * Если >=2 точек и они выстроены в один столбец → enqueue ровно 2 клетки: сверху от min_row и снизу от max_row.
     * Если пока нет «едиой прямой» (например, попали в (2,3) и в (5,7) – не по одной линии) → enqueue ортососеди для последнего попадания.
     */
    private fun enqueueBasedOnHits() {
        // Сначала очистим предыдущие кандидаты
        targetQueue.clear()

        if (hitCells.size == 1) {
            // Только одна точка – просто ставим в очередь её 4 ортогональных соседа
            val (r, c) = hitCells.first()
            enqueueOrthogonal(r, c)
            return
        }

        // Проверим: все ли точки лежат в одной строке?
        val sameRow = hitCells.map { it.first }.distinct().size == 1
        // Проверим: все ли точки лежат в одном столбце?
        val sameCol = hitCells.map { it.second }.distinct().size == 1

        if (sameRow) {
            // Горизонтальный корабль
            val row = hitCells.first().first
            val sortedCols = hitCells.map { it.second }.sorted()
            val leftC = sortedCols.first()
            val rightC = sortedCols.last()

            // Сосед слева:
            val leftCell = row to (leftC - 1)
            if (leftCell in availableCells) {
                targetQueue.addLast(leftCell)
            }
            // Сосед справа:
            val rightCell = row to (rightC + 1)
            if (rightCell in availableCells) {
                targetQueue.addLast(rightCell)
            }
        } else {
            // Вертикальный корабль
            val col = hitCells.first().second
            val sortedRows = hitCells.map { it.first }.sorted()
            val topR = sortedRows.first()
            val bottomR = sortedRows.last()

            // Сосед сверху:
            val upCell = (topR - 1) to col
            if (upCell in availableCells) {
                targetQueue.addLast(upCell)
            }
            // Сосед снизу:
            val downCell = (bottomR + 1) to col
            if (downCell in availableCells) {
                targetQueue.addLast(downCell)
            }
        }
    }

    /**
     * Добавляет в очередь добивания все 4 ортогональных соседа (r,c),
     * которых ещё нет в targetQueue и которые есть в availableCells.
     */
    private fun enqueueOrthogonal(r: Int, c: Int) {
        listOf(
            r to (c + 1),
            r to (c - 1),
            (r + 1) to c,
            (r - 1) to c
        ).forEach { cell ->
            if (cell in availableCells && cell !in targetQueue) {
                targetQueue.addLast(cell)
            }
        }
    }

    /**
     * Строит «буфер» (вокруг каждой точки shipCells) – все 8 соседних клеток,
     * исключая сами shipCells. Возвращает Set<Pair<row,col>>.
     */
    private fun computeBuffer(shipCells: List<Pair<Int, Int>>): Set<Pair<Int, Int>> {
        val buffer = mutableSetOf<Pair<Int, Int>>()
        shipCells.forEach { (r, c) ->
            for (dr in -1..1) {
                for (dc in -1..1) {
                    if (dr == 0 && dc == 0) continue
                    val nr = r + dr
                    val nc = c + dc
                    if (nr in 0..9 && nc in 0..9) {
                        buffer.add(nr to nc)
                    }
                }
            }
        }
        buffer.removeAll(shipCells)
        return buffer
    }

    /** Полный сброс всего внутреннего режима «добивания» */
    private fun resetHuntMode() {
        targetQueue.clear()
        hitCells.clear()
    }
}
