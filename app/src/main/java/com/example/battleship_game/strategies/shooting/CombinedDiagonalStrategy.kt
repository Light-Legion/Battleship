package com.example.battleship_game.strategies.shooting

import kotlin.random.Random

/**
 * Стратегия "Комбинированная диагональ" (уровень MEDIUM)
 *
 * Логика:
 * 1) Если есть “hitStack” (режим добивания) — добиваем сначала концами, потом соседями, потом nearMisses.
 * 2) Иначе (нет активных попаданий) —
 *    • Сначала выполняем «диагональные» ходы с учётом «паритета» (чем больше корабль, тем паритет 2, иначе 3).
 *    • Если за N подряд промахов (адаптивный threshold) диагонали кончились, — переходим в “расширенный” поиск:
 *      • сначала углы, потом границы, потом оставшиеся.
 *
 * При каждом попадании / промахе обновляем: board, hitStack, consecutiveMisses, remainingShips.
 */
class CombinedDiagonalStrategy : BaseShootingStrategy() {

    companion object {
        private const val SIZE = 10

        // Ортогональные смещения
        private val DIRS = listOf(
            1 to 0,
            -1 to 0,
            0 to 1,
            0 to -1
        )

        // Набор длин всех кораблей (4,3,3,2,2,2,1,1,1,1)
        private val INITIAL_SHIPS = listOf(4, 3, 3, 2, 2, 2, 1, 1, 1, 1)
    }

    // Состояние каждой клетки: UNKNOWN / MISS / HIT / SUNK / BUFFER
    private val board = Array(SIZE) { Array(SIZE) { CellState.UNKNOWN } }

    // Счётчик подряд идущих промахов (для адаптивного threshold)
    private var consecutiveMisses = 0

    // Флаг, что мы ещё “в фазе диагоналей”
    private var diagonalPhase = true

    // Список попаданий, из которых формируется «режим добивания»
    private val hitStack = mutableListOf<Pair<Int, Int>>()

    // Текущие длины кораблей, которые ещё не потоплены
    private val remainingShips = INITIAL_SHIPS.toMutableList()

    override fun computeNextShot(): Pair<Int, Int> {
        // ==================================================
        // 1) РЕЖИМ ДОБИВАНИЯ: если hitStack не пуст, сначала добиваем
        // ==================================================
        if (hitStack.isNotEmpty()) {
            // Найдём две крайние точки в hitStack
            val (r1, c1) =
                hitStack.minWithOrNull(compareBy({ it.first }, { it.second }))
                    ?: return fallbackShot()
            val (r2, c2) =
                hitStack.maxWithOrNull(compareBy({ it.first }, { it.second }))
                    ?: return fallbackShot()

            val horizontal = (r1 == r2)
            // Шаги “за концы”
            val candidates = mutableListOf<Pair<Int, Int>>().apply {
                if (horizontal) {
                    add(r1 to (c1 - 1))
                    add(r1 to (c2 + 1))
                } else {
                    add((r1 - 1) to c1)
                    add((r2 + 1) to c1)
                }
            }
            // 1.1) Попытаться выстрелить за концом, если там свободно
            candidates.shuffled().firstOrNull { (r, c) ->
                isCellAvailable(r, c)
            }?.let { return it }

            // 1.2) Если за концами ничего нет — проверяем «nearMisses»
            val near = findNearMisses()
            near.shuffled().firstOrNull { (r, c) ->
                isCellAvailable(r, c)
            }?.let { return it }

            // 1.3) Наконец пробуем просто всех соседей
            DIRS.shuffled().forEach { (dr, dc) ->
                hitStack.shuffled().forEach { (r, c) ->
                    val nr = r + dr
                    val nc = c + dc
                    if (isCellAvailable(nr, nc)) {
                        return nr to nc
                    }
                }
            }
            // Если и тут всё занято — FALLTHROUGH в режим «диагоналей или расширенного»
        }

        // ==================================================
        // 2) РЕЖИМ ДИАГОНАЛЕЙ
        // ==================================================
        // Адаптивный threshold: чем меньше самый крупный корабль, тем меньше промахов нужно для перехода
        val largest = remainingShips.maxOrNull() ?: 1
        val dynamicThreshold = when (largest) {
            4 -> 14
            3 -> 12
            2 -> 8
            else -> 6
        }
        if (consecutiveMisses >= dynamicThreshold) {
            diagonalPhase = false
        }

        if (diagonalPhase) {
            // Выбираем паритет: для крупных кораблей ходим по (r + c) % 2 == 0,
            // а если остались только маленькие (длина 2 или 1) — (r + c) % 3 == 0
            val parity = if (largest >= 3) 2 else 3
            for (r in 0 until SIZE) {
                for (c in 0 until SIZE) {
                    if (((r + c) % parity == 0) && isCellAvailable(r, c)) {
                        return r to c
                    }
                }
            }
            // Если все подходящие “паритетные” клетки заняты, переключаемся на расширенный поиск
            diagonalPhase = false
        }

        // ==================================================
        // 3) РАСШИРЕННЫЙ ПОИСК (углы → края → любые оставшиеся)
        // ==================================================
        // 3.1) Сначала четыре угла:
        val corners = listOf(
            0 to 0,
            0 to (SIZE - 1),
            (SIZE - 1) to 0,
            (SIZE - 1) to (SIZE - 1)
        ).shuffled()
        corners.firstOrNull { (r, c) ->
            isCellAvailable(r, c)
        }?.let { return it }

        // 3.2) Потом все клетки на границах (верх/низ/лево/право):
        val edges = mutableListOf<Pair<Int, Int>>().apply {
            for (i in 0 until SIZE) {
                add(0        to i)
                add((SIZE-1) to i)
                add(i        to 0)
                add(i        to (SIZE-1))
            }
        }.shuffled()
        edges.firstOrNull { (r, c) ->
            isCellAvailable(r, c)
        }?.let { return it }

        // 3.3) Наконец — любая оставшаяся клетка:
        return findFirstAvailableCell() ?: fallbackShot()
    }

    override fun onShotResult(lastShot: Pair<Int, Int>?, hit: Boolean, sunk: Boolean) {
        lastShot?.let { (r, c) ->
            when {
                // ─── 1) Попал и **сразу** потопил корабль
                hit && sunk -> {
                    // Отметим текущее попадание как HIT
                    board[r][c] = CellState.HIT
                    hitStack.add(r to c)
                    // Теперь во hitStack собраны ВСЕ точки этого нового корабля
                    // Переведём их все в SUNK
                    hitStack.forEach { (hr, hc) ->
                        board[hr][hc] = CellState.SUNK
                    }
                    // Нанесём буфер вокруг всех этих точек
                    markBufferAround(hitStack)
                    // Удалим из remainingShips ту длину, которую только что потопили
                    remainingShips.remove(hitStack.size)
                    // Очистим hitStack — этот корабль добит!
                    hitStack.clear()
                    consecutiveMisses = 0
                }

                // ─── 2) Просто попал (но не до конца)
                hit -> {
                    board[r][c] = CellState.HIT
                    if ((r to c) !in hitStack) {
                        hitStack.add(r to c)
                    }
                    consecutiveMisses = 0
                }

                // ─── 3) Промах
                else -> {
                    board[r][c] = CellState.MISS
                    consecutiveMisses++
                }
            }
        }
    }

    /** Проверяет, что (row, col) в пределах поля и не тронута (UNKNOWN). */
    private fun isCellAvailable(row: Int, col: Int): Boolean {
        return row in 0 until SIZE
                && col in 0 until SIZE
                && board[row][col] == CellState.UNKNOWN
    }

    /**
     * Помечает вокруг всех точек из hits («палуб» потопленного корабля)
     * зоны BUFFER (и переводит сами hits в SUNK).
     */
    private fun markBufferAround(hits: List<Pair<Int, Int>>) {
        if (hits.isEmpty()) return

        // Проверим, лежат ли все точки на одной строке (горизонтальный корабль)
        val horizontal = hits.size >= 2 && hits.all { it.first == hits[0].first }
        if (horizontal) {
            val row = hits[0].first
            val cols = hits.map { it.second }.sorted()
            val cStart = cols.first()
            val cEnd   = cols.last()

            // 1) Соседи по бокам (строки row-1 и row+1, от cStart-1 до cEnd+1)
            for (c in (cStart - 1)..(cEnd + 1)) {
                if (c in 0 until SIZE) {
                    if (row - 1 in 0 until SIZE && board[row - 1][c] == CellState.UNKNOWN) {
                        board[row - 1][c] = CellState.BUFFER
                    }
                    if (row + 1 in 0 until SIZE && board[row + 1][c] == CellState.UNKNOWN) {
                        board[row + 1][c] = CellState.BUFFER
                    }
                }
            }
            // 2) Углы
            if (cStart - 1 >= 0) {
                if (row - 1 in 0 until SIZE && board[row - 1][cStart - 1] == CellState.UNKNOWN)
                    board[row - 1][cStart - 1] = CellState.BUFFER
                if (row + 1 in 0 until SIZE && board[row + 1][cStart - 1] == CellState.UNKNOWN)
                    board[row + 1][cStart - 1] = CellState.BUFFER
            }
            if (cEnd + 1 < SIZE) {
                if (row - 1 in 0 until SIZE && board[row - 1][cEnd + 1] == CellState.UNKNOWN)
                    board[row - 1][cEnd + 1] = CellState.BUFFER
                if (row + 1 in 0 until SIZE && board[row + 1][cEnd + 1] == CellState.UNKNOWN)
                    board[row + 1][cEnd + 1] = CellState.BUFFER
            }
        } else {
            // Вертикальный корабль
            val col = hits[0].second
            val rows = hits.map { it.first }.sorted()
            val rStart = rows.first()
            val rEnd   = rows.last()

            // 1) Соседи по бокам (столбцы col-1 и col+1, от rStart-1 до rEnd+1)
            for (r in (rStart - 1)..(rEnd + 1)) {
                if (r in 0 until SIZE) {
                    if (col - 1 in 0 until SIZE && board[r][col - 1] == CellState.UNKNOWN) {
                        board[r][col - 1] = CellState.BUFFER
                    }
                    if (col + 1 in 0 until SIZE && board[r][col + 1] == CellState.UNKNOWN) {
                        board[r][col + 1] = CellState.BUFFER
                    }
                }
            }
            // 2) Углы
            if (rStart - 1 >= 0) {
                if (col - 1 in 0 until SIZE && board[rStart - 1][col - 1] == CellState.UNKNOWN)
                    board[rStart - 1][col - 1] = CellState.BUFFER
                if (col + 1 in 0 until SIZE && board[rStart - 1][col + 1] == CellState.UNKNOWN)
                    board[rStart - 1][col + 1] = CellState.BUFFER
            }
            if (rEnd + 1 < SIZE) {
                if (col - 1 in 0 until SIZE && board[rEnd + 1][col - 1] == CellState.UNKNOWN)
                    board[rEnd + 1][col - 1] = CellState.BUFFER
                if (col + 1 in 0 until SIZE && board[rEnd + 1][col + 1] == CellState.UNKNOWN)
                    board[rEnd + 1][col + 1] = CellState.BUFFER
            }
        }

        // В конце ещё раз убедимся, что hit‐точки помечены SU NK
        hits.forEach { (r, c) ->
            board[r][c] = CellState.SUNK
        }
    }

    /**
     * Собирает список клеток (row, col) “рядом” с промахами:
     * вокруг каждой MISS смотрит 4 соседей,
     * и если они UNKNOWN – отдаёт их как кандидатов.
     */
    private fun findNearMisses(): List<Pair<Int, Int>> {
        val near = mutableListOf<Pair<Int, Int>>()
        for (r in 0 until SIZE) {
            for (c in 0 until SIZE) {
                if (board[r][c] == CellState.MISS) {
                    DIRS.forEach { (dr, dc) ->
                        val nr = r + dr
                        val nc = c + dc
                        if (isCellAvailable(nr, nc)) {
                            near.add(nr to nc)
                        }
                    }
                }
            }
        }
        return near
    }

    /** Ищет любую первую клетку с состоя­нием UNKNOWN. */
    private fun findFirstAvailableCell(): Pair<Int, Int>? {
        for (r in 0 until SIZE) {
            for (c in 0 until SIZE) {
                if (isCellAvailable(r, c)) {
                    return r to c
                }
            }
        }
        return null
    }

    /**
     * Если вдруг мы не нашли ни одного “хорошего” кандидата, возвращаем
     * случайную (row, col). Теоретически такого не произойдёт, т.к. tried[]
     * отсекает уже использованные, но на всякий случай:
     */
    private fun fallbackShot(): Pair<Int, Int> {
        return Random.nextInt(0, SIZE) to Random.nextInt(0, SIZE)
    }
}