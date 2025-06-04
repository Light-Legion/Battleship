package com.example.battleship_game.strategies.shooting

import kotlin.random.Random

/**
 * Стратегия «Сложный» с анализом плотности (Heatmap) и адаптивными коррекциями.
 *
 * Теперь включает этап «Эвристического угадывания»: после 10 подряд промахов
 * запрашивает _у ViewModel_ одну случайную палубу из ещё непотопленных кораблей игрока.
 *
 * Как работает:
 * ─────────────────────────────────────────────────────────────────────────────
 * 1) Hunt-режим (добивание):
 *    • Если в huntQueue есть координаты (enqueueOrthogonal()/enqueueBasedOnHits()),
 *      возвращаем первую доступную и выходим.
 *
 * 2) Если нет активного Hunt-режима:
 *    a) Если подряд промахов (consecutiveMisses) ≥ GUESS_BONUS_THRESHOLD (10),
 *       — вызываем callback `enemyShipProvider()`, получаем актуальный список (row, col)
 *         всех непотопленных палуб игрока.
 *       — из него фильтруем те, по которым ещё не стреляли, и сразу возвращаем одну случайную.
 *       — Если список пуст (все уже пробиты) — переходим к Heatmap-режиму.
 *
 *    b) Если подряд промахов < GUESS_BONUS_THRESHOLD:
 *       — строим «тепловую карту» (Heatmap) для всех оставшихся кораблей (remainingShips).
 *         Если подряд промахов ≥ EDGE_BONUS_THRESHOLD (6), добавляем +2 «бонус» к весам пограничных клеток.
 *       — среди всех EMPTY-клеток выбираем те с максимальным счётом и случайно возвращаем одну.
 *
 * 3) onShotResult(lastShot, hit, sunk):
 *    — Если hit && sunk:
 *        • Добавляем (r,c) в huntHits (если ещё нет).
 *        • Определяем длину макс. оставшегося корабля → justSunkLen.
 *        • Ищем в huntHits ровную цепочку длиной justSunkLen (extractSunkChain).
 *        • Помечаем её как SUNK и строим буфер MISS вокруг (markBufferAround).
 *        • Удаляем длину корабля из remainingShips, сбрасываем Hunt-режим и обнуляем consecutiveMisses.
 *
 *    — Если просто hit (не sunk):
 *        • board[r][c] = HIT, huntHits.add((r,c)), enqueueBasedOnHits(), consecutiveMisses = 0.
 *
 *    — Если miss:
 *        • board[r][c] = MISS, consecutiveMisses++.
 *
 * Порядок приоритетов: **hunt-режим > эвристическое угадывание > heatmap**.
 */
class DensityAnalysisStrategy : BaseShootingStrategy() {

    companion object {
        /** Порог промахов, при котором начинаем «усилять» края. */
        private const val EDGE_BONUS_THRESHOLD = 10
    }

    /** Виртуальное поле (SIZE×SIZE) для учёта MISS/HIT/SUNK/EMPTY. */
    private val board = Array(SIZE) { Array(SIZE) { CellState.EMPTY } }

    /** Длины тех кораблей игрока, что ещё не потоплены. */
    private val remainingShips = INITIAL_SHIPS.toMutableList()

    /** Счётчик подряд промахов. */
    private var consecutiveMisses = 0

    // ─────────────────────────────────────────────────────────────────────────────
    // 1) computeNextShot() — «сырая» логика выбора следующей клетки (без учёта tried[][]).
    //    getNextShot() проверит isValidCell и !hasTried.
    // ─────────────────────────────────────────────────────────────────────────────
    override fun computeNextShot(): Pair<Int, Int> {
        // ——— 1) Hunt-режим (добивание) ———
        if (huntQueue.isNotEmpty()) {
            while (huntQueue.isNotEmpty()) {
                val (rr, cc) = huntQueue.removeFirst()
                if (isValidCell(rr, cc)
                    && board[rr][cc] == CellState.EMPTY
                    && !hasTried(rr, cc)
                ) {
                    return rr to cc
                }
            }
            // Если все кандидаты устарели — сбрасываем очередь
            huntQueue.clear()
        }


        // 2) Heatmap-режим (+ бонус крайним клеткам, если consecutiveMisses ≥ EDGE_BONUS_THRESHOLD)
        return computeHeatmapShot()
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 2) onShotResult() — обновление внутреннего состояния после выстрела
    // ─────────────────────────────────────────────────────────────────────────────
    override fun onShotResult(lastShot: Pair<Int, Int>?, hit: Boolean, sunk: Boolean) {
        if (lastShot == null) return
        val (r, c) = lastShot

        when {
            // ―― 2.1) Попадание + сразу Sunken (корабль потоплен) ――
            hit && sunk -> {
                // Добавляем (r,c) в huntHits, если его там ещё нет
                if ((r to c) !in huntHits) {
                    huntHits.add(r to c)
                }
                // Определяем длину только что потопленного корабля
                val chain = findSunkChain(huntHits, r to c) ?: listOf(r to c)
                val justSunkLen = chain.size

                // Помечаем цепочку как SUNK и строим буфер MISS вокруг неё
                markBufferAround(chain)
                // Удаляем длину потопленного корабля из remainingShips
                remainingShips.remove(justSunkLen)
                // Сбрасываем Hunt-режим (huntQueue, huntHits)
                resetHuntMode()
                // Сбрасываем consecutiveMisses
                consecutiveMisses = 0
            }

            // ―― 2.2) Просто Hit (корабль ещё не потоплен) ――
            hit -> {
                board[r][c] = CellState.HIT
                huntHits.add(r to c)
                enqueueBasedOnHits()
                // Сбрасываем счётчик промахов
                consecutiveMisses = 0
            }

            // ―― 2.3) Miss ――
            else -> {
                board[r][c] = CellState.MISS
                consecutiveMisses++
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 3) Строим «тепловую карту» (Heatmap), при необходимости корректируем веса пограничных клеток
    // ─────────────────────────────────────────────────────────────────────────────
    private fun computeHeatmapShot(): Pair<Int, Int> {
        // [1] Инициализация массива весов
        val counts = Array(SIZE) { IntArray(SIZE) }

        // [2] Для каждой длины length из remainingShips перебираем все валидные вставки
        for (length in remainingShips) {
            val includeVertical = (length > 1)
            // Определяем «вес» этого корабля. Например, weight = length.
            // (Можно сделать более сложную функцию weight(length), если надо.)
            val shipWeight = length

            // — Горизонтальные варианты
            for (r in 0 until SIZE) {
                for (c in 0..(SIZE - length)) {
                    var canPlace = true
                    for (k in 0 until length) {
                        if (board[r][c + k] != CellState.EMPTY) {
                            canPlace = false
                            break
                        }
                    }
                    if (canPlace) {
                        for (k in 0 until length) {
                            counts[r][c + k] += shipWeight
                        }
                    }
                }
            }

            if (includeVertical) {
                // — Вертикальные варианты
                for (c in 0 until SIZE) {
                    for (r in 0..(SIZE - length)) {
                        var canPlace = true
                        for (k in 0 until length) {
                            if (board[r + k][c] != CellState.EMPTY) {
                                canPlace = false
                                break
                            }
                        }
                        if (canPlace) {
                            for (k in 0 until length) {
                                counts[r + k][c]++
                            }
                        }
                    }
                }
            }
        }

        // [3] Если подряд промахов ≥ EDGE_BONUS_THRESHOLD (6) и < GUESS_BONUS_THRESHOLD (10),
        // усиливаем пограничные клетки на +2
        if (consecutiveMisses >= EDGE_BONUS_THRESHOLD) {
            for (i in 0 until SIZE) {
                if (board[0][i] == CellState.EMPTY)           counts[0][i]           += 10
                if (board[SIZE - 1][i] == CellState.EMPTY)    counts[SIZE - 1][i]    += 10
                if (board[i][0] == CellState.EMPTY)           counts[i][0]           += 10
                if (board[i][SIZE - 1] == CellState.EMPTY)    counts[i][SIZE - 1]    += 10
            }
        }

        // [4] Находим максимальный вес среди EMPTY-клеток
        var maxCount = 0
        for (r in 0 until SIZE) {
            for (c in 0 until SIZE) {
                if (board[r][c] == CellState.EMPTY && counts[r][c] > maxCount) {
                    maxCount = counts[r][c]
                }
            }
        }

        // [5] Собираем кандидатов с weight == maxCount
        val candidates = mutableListOf<Pair<Int, Int>>()
        for (r in 0 until SIZE) {
            for (c in 0 until SIZE) {
                if (board[r][c] == CellState.EMPTY && counts[r][c] == maxCount) {
                    candidates.add(r to c)
                }
            }
        }

        // [6] Если кандидаты найдены — выбираем случайную
        if (candidates.isNotEmpty()) {
            return candidates.random(Random.Default)
        }

        // [7] В противном случае (редкий крайний случай) — возвращаем первую доступную EMPTY
        return findFirstEmptyCell() ?: throw IllegalStateException("No empty cell left")
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 4) Находит первую пустую (EMPTY && !hasTried) клетку, если heatmap-запрос никого не вернул
    // ─────────────────────────────────────────────────────────────────────────────
    private fun findFirstEmptyCell(): Pair<Int, Int>? {
        for (r in 0 until SIZE) {
            for (c in 0 until SIZE) {
                if (board[r][c] == CellState.EMPTY && !hasTried(r, c)) {
                    return r to c
                }
            }
        }
        return null
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 5) Ищем «цепочку» hit-точек (горизонтально или вертикально)
    // ─────────────────────────────────────────────────────────────────────────────
    private fun findSunkChain(
        hits: List<Pair<Int, Int>>,
        start: Pair<Int, Int>
    ): List<Pair<Int, Int>>? {
        val (r, c) = start
        // Находим горизонтальную цепочку
        var left = c
        while (left > 0 && (r to left - 1) in hits) left--
        var right = c
        while (right < SIZE - 1 && (r to right + 1) in hits) right++
        val horizontalChain = (left..right).map { r to it }

        // Находим вертикальную цепочку
        var up = r
        while (up > 0 && (up - 1 to c) in hits) up--
        var down = r
        while (down < SIZE - 1 && (down + 1 to c) in hits) down++
        val verticalChain = (up..down).map { it to c }

        // Выбираем цепочку с большей длиной, если длины равны, выбираем горизонтальную
        return if (horizontalChain.size >= verticalChain.size) horizontalChain else verticalChain
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 6) Помечаем цепочку hits как SUNK и строим буфер MISS вокруг каждой точки
    // ─────────────────────────────────────────────────────────────────────────────
    private fun markBufferAround(hits: List<Pair<Int, Int>>) {
        if (hits.isEmpty()) return

        // [1] Маркируем сами hits как SUNK
        for ((r, c) in hits) {
            board[r][c] = CellState.SUNK
        }
        // [2] Вокруг каждой точки рисуем буфер MISS (8 соседей)
        for ((r, c) in hits) {
            for (dr in -1..1) {
                for (dc in -1..1) {
                    if (dr == 0 && dc == 0) continue
                    val nr = r + dr
                    val nc = c + dc
                    if (nr in 0 until SIZE && nc in 0 until SIZE && board[nr][nc] == CellState.EMPTY) {
                        board[nr][nc] = CellState.MISS
                    }
                }
            }
        }
    }
}
