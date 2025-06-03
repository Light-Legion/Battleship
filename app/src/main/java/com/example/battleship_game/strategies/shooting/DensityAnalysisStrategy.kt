package com.example.battleship_game.strategies.shooting

import kotlin.math.exp

/**
 * Стратегия "Анализ плотности" (уровень HARD).
 *
 * Логика:
 * 1) Если есть незавершённая цепочка попаданий (currentHits),
 *    то продолжаем добивание точно так же, как в CombinedDiagonalStrategy:
 *    а) пробуем «за концами»,
 *    б) если не нашли — соседние клетки,
 *    в) завершаем цепочку, если потоплен.
 *
 * 2) Иначе — строим «тепловую карту» (heatMap) по текущему состоянию board[] и списку remainingShips:
 *    Для каждого ship в remainingShips перебираем все валидные положения (горизонтальные / вертикальные).
 *    В каждую клетку этого положения додаём вес = weight[row][col].
 *    Итоговая heatMap[row][col] = сумма всех “вписок” кораблей через эту клетку × вес[row][col].
 *    Выбираем клетку (row,col) с максимальным значением.
 *
 * При каждом сообщении onShotResult(lastShot, hit, sunk) мы:
 *  - обновляем board[row][col] = HIT или MISS
 *  - если hit && sunk → помечаем все текущие currentHits как SUNK + строим BUFFER, удаляем корабль из remainingShips, очищаем currentHits
 *  - если hit && !sunk → помечаем board[row][col] = HIT, добавляем в currentHits (если ещё нет)
 *  - если miss → помечаем board[row][col] = MISS
 *  В конце каждого ветвления устанавливаем needsRecalculation = true
 */
class DensityAnalysisStrategy : BaseShootingStrategy() {

    companion object {
        private const val SIZE = 10
        private val DIRS = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)
        private val INITIAL_SHIPS = listOf(4, 3, 3, 2, 2, 2, 1, 1, 1, 1)
    }

    /** board[row][col]: текущее состояние клетки */
    private val board = Array(SIZE) { Array(SIZE) { CellState.UNKNOWN } }

    /** Список длин кораблей, которые ещё не потоплены */
    private val remainingShips = INITIAL_SHIPS.toMutableList()

    /** Список координат текущих «попаданий», пока корабль не потонул */
    private val currentHits = mutableListOf<Pair<Int, Int>>()

    /** Кэш “тепловой карты” */
    private var heatMap: Array<DoubleArray> = Array(SIZE) { DoubleArray(SIZE) { 0.0 } }

    /** Флаг, что нужно пересчитать heatMap */
    private var needsRecalculation = true

    /**
     * 1) Hunt‐mode: если currentHits не пуст, добиваем.
     * 2) Fire‐mode: иначе (пересоздаём heatMap, если нужно) и берём (row, col) с макс. heatMap.
     */
    override fun computeNextShot(): Pair<Int, Int> {
        // ─── 1) Режим добивания ───
        if (currentHits.isNotEmpty()) {
            // Найдём «края» цепочки currentHits:
            val (r1, c1) =
                currentHits.minWithOrNull(compareBy({ it.first }, { it.second }))
                    ?: return findBestCell()
            val (r2, c2) =
                currentHits.maxWithOrNull(compareBy({ it.first }, { it.second }))
                    ?: return findBestCell()

            val horizontal = (r1 == r2)
            // 1.1) Попытки «за концами»
            val candidates = mutableListOf<Pair<Int, Int>>().apply {
                if (horizontal) {
                    add(r1 to (c1 - 1))
                    add(r1 to (c2 + 1))
                } else {
                    add((r1 - 1) to c1)
                    add((r2 + 1) to c1)
                }
            }
            candidates.shuffled().firstOrNull { (r, c) ->
                isCellAvailable(r, c)
            }?.let { return it }

            // 1.2) Потом все ортогональные соседи
            DIRS.shuffled().forEach { (dr, dc) ->
                currentHits.shuffled().forEach { (r, c) ->
                    val nr = r + dr
                    val nc = c + dc
                    if (isCellAvailable(nr, nc)) {
                        return nr to nc
                    }
                }
            }
            // FALLTHROUGH → если не нашли вообще, идём в heatMap‐режим
        }

        // ─── 2) Режим «тепловой карты» ───
        if (needsRecalculation) {
            heatMap = computeHeatMap()
            needsRecalculation = false
        }
        return findBestCell()
    }

    /**
     * После каждого выстрела ViewModel вызывает этот метод,
     * передавая (row, col, hit, sunk). Мы обновляем board[] и внутренние списки.
     */
    override fun onShotResult(
        lastShot: Pair<Int, Int>?,
        hit: Boolean,
        sunk: Boolean
    ) {
        lastShot?.let { (r, c) ->
            when {
                // ─── 1) Попал + Потопил ───
                hit && sunk -> {
                    // 1.1) Временно отметим эту клетку как HIT
                    board[r][c] = CellState.HIT
                    currentHits.add(r to c)

                    // 1.2) Теперь в currentHits – ВСЕ палубы потопленного корабля
                    //       Переводим их в SUNK:
                    currentHits.forEach { (hr, hc) ->
                        board[hr][hc] = CellState.SUNK
                    }

                    // 1.3) Пометить буфер вокруг всех точек (hr, hc) из currentHits
                    markBufferAround(currentHits)

                    // 1.4) Из remainingShips удаляем длину этого корабля
                    remainingShips.remove(currentHits.size)

                    // 1.5) Очищаем currentHits
                    currentHits.clear()

                    // 1.6) Больше не верим в старую heatMap
                    needsRecalculation = true
                }

                // ─── 2) Просто попал (но корабль не допотоплён) ───
                hit -> {
                    if ((r to c) !in currentHits) {
                        board[r][c] = CellState.HIT
                        currentHits.add(r to c)
                    }
                    needsRecalculation = true
                }

                // ─── 3) Промах ───
                else -> {
                    board[r][c] = CellState.MISS
                    needsRecalculation = true
                }
            }
        }
    }

    /** Проверяет, что (row, col) в пределах и ещё не тронута (UNKNOWN). */
    private fun isCellAvailable(row: Int, col: Int): Boolean {
        return row in 0 until SIZE
                && col in 0 until SIZE
                && board[row][col] == CellState.UNKNOWN
    }

    /**
     * Помечает вокруг всех пар (hr, hc) из hits:
     *  - клетки сразу по бокам и углам (BUFFER)
     *  - сами “hits” переводим в SUNK (на всякий случай).
     */
    private fun markBufferAround(hits: List<Pair<Int, Int>>) {
        if (hits.isEmpty()) return

        // Определим, горизонтальный или вертикальный это отрезок
        val horizontal = hits.size >= 2 && hits.all { it.first == hits[0].first }
        if (horizontal) {
            val row = hits[0].first
            val cols = hits.map { it.second }.sorted()
            val cStart = cols.first()
            val cEnd   = cols.last()

            // 1) Соседи по бокам:
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
            // 2) Углы:
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
            // Вертикальный отрезок
            val col = hits[0].second
            val rows = hits.map { it.first }.sorted()
            val rStart = rows.first()
            val rEnd   = rows.last()

            // 1) Соседи по бокам:
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
            // 2) Углы:
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

        // Снова пометим сами hits как SUNK (на всякий случай, если где-то остался HIT)
        hits.forEach { (hr, hc) ->
            board[hr][hc] = CellState.SUNK
        }
    }

    /**
     * Считает “тепловую карту” (heatMap) для ВСЕХ клеток.
     * Проходим через каждый shipLength из remainingShips и «вписываем» его горизонтально/вертикально,
     * сканируем board[]:
     *  - Если любая клетка в потенциальном положении мешает (MISS / BUFFER / SUNK) → пропускаем.
     *  - Если встречаем HIT, этот HIT должен входить в currentHits → иначе тоже пропускаем.
     * Для всех допустимых расположений добавляем weight[row][col] к heat[row][col].
     */
    private fun computeHeatMap(): Array<DoubleArray> {
        val heat = Array(SIZE) { DoubleArray(SIZE) }
        val weight = generateWeightMap()

        for (shipLength in remainingShips) {
            // Попробуем все горизонтальные «вписывания»
            for (r in 0 until SIZE) {
                for (c in 0..(SIZE - shipLength)) {
                    var valid = true
                    for (k in 0 until shipLength) {
                        val state = board[r][c + k]
                        if (state == CellState.MISS
                            || state == CellState.BUFFER
                            || state == CellState.SUNK
                        ) {
                            valid = false
                            break
                        }
                        if (state == CellState.HIT && (r to (c + k) !in currentHits)) {
                            valid = false
                            break
                        }
                    }
                    if (valid) {
                        for (k in 0 until shipLength) {
                            heat[r][c + k] += weight[r][c + k]
                        }
                    }
                }
            }
            // Попробуем все вертикальные «вписывания»
            for (r in 0..(SIZE - shipLength)) {
                for (c in 0 until SIZE) {
                    var valid = true
                    for (k in 0 until shipLength) {
                        val state = board[r + k][c]
                        if (state == CellState.MISS
                            || state == CellState.BUFFER
                            || state == CellState.SUNK
                        ) {
                            valid = false
                            break
                        }
                        if (state == CellState.HIT && ((r + k) to c) !in currentHits) {
                            valid = false
                            break
                        }
                    }
                    if (valid) {
                        for (k in 0 until shipLength) {
                            heat[r + k][c] += weight[r + k][c]
                        }
                    }
                }
            }
        }
        return heat
    }

    /**
     * Генерирует «весовую» карту weight[row][col] по идее “Гаусса”,
     * чтобы центр поля имел чуть больший вес, края – меньший.
     */
    private fun generateWeightMap(): Array<DoubleArray> {
        val heat = Array(SIZE) { DoubleArray(SIZE) }
        val center = (SIZE - 1) / 2.0  // 4.5
        val sigma = SIZE / 4.0        // примерно 2.5

        for (r in 0 until SIZE) {
            for (c in 0 until SIZE) {
                val dr = r - center
                val dc = c - center
                val gaussian = exp(-(dr * dr + dc * dc) / (2.0 * sigma * sigma))
                // Переводим в диапазон [0.6 .. 1.0]
                heat[r][c] = 0.6 + 0.4 * gaussian
            }
        }
        return heat
    }

    /**
     * Находит клетку (row, col) с максимальным значением heatMap,
     * которая всё ещё UNKNOWN.
     */
    private fun findBestCell(): Pair<Int, Int> {
        var bestR = 0
        var bestC = 0
        var maxVal = -1.0
        for (r in 0 until SIZE) {
            for (c in 0 until SIZE) {
                if (board[r][c] == CellState.UNKNOWN) {
                    val v = heatMap[r][c]
                    if (v > maxVal) {
                        maxVal = v
                        bestR = r
                        bestC = c
                    }
                }
            }
        }
        return bestR to bestC
    }
}
