package com.example.battleship_game.strategies.shooting

import kotlin.random.Random

/**
 * Стратегия «Диагональная с вероятностным расширенным поиском».
 *
 * 1) **Hunt-режим (добивание)**
 *    — Если в базовом huntQueue есть координаты (после попадания) → стреляем по ним
 *      (используется enqueueOrthogonal() и enqueueBasedOnHits() из BaseShootingStrategy).
 *
 * 2) **Диагональная фаза (пока подряд промахов < dynamicThreshold)**
 *    — Заранее задаём порядок точек:
 *       • mainShots  = [(0,0),(9,9),(2,2),(7,7),(4,4),(5,5)]
 *       • secondaryShots = [(0,9),(9,0),(2,7),(7,2),(4,5),(5,4)]
 *    — Пока подряд промахов меньше порога (dynamicThreshold зависит от длины самого большого живого корабля):
 *       • сначала стреляем по mainShots в указанном порядке,
 *       • затем — по secondaryShots.
 *    — Как только либо списки исчерпались, либо промахов подряд стало ≥ dynamicThreshold,
 *      выключаем diagonalPhase и переходим в probability-режим.
 *
 * 3) **Probability Mode («тепловая матрица»)**
 *    — Берём список оставшихся кораблей remainingShips (их длины).
 *    — Строим двумерный массив counts[SIZE][SIZE] = 0.
 *    — Для каждого корабля длины L перебираем все возможные горизонтальные и вертикальные «вставки»:
 *       • Если все L ячеек свободны (CellState.EMPTY), то для каждой из них делаем counts[++].
 *    — В конце выбираем любую EMPTY-клетку, у которой counts максимален, и стреляем в неё.
 *
 * После каждого выстрела (onShotResult()):
 *  — Если (hit && sunk):
 *       • Добавляем (r,c) в huntHits (если ещё нет).
 *       • Вычисляем цепочку попаданий нужной длины (extractSunkChain).
 *       • Помечаем эти клетки как SUNK + строим вокруг буфер MISS (markBufferAround).
 *       • Удаляем длину корабля из remainingShips, сбрасываем hunt-режим, обнуляем consecutiveMisses.
 *  — Если (hit но не sunk):
 *       • Помечаем board[r][c] = HIT, добавляем (r,c) в huntHits, вызываем enqueueBasedOnHits(), сбрасываем consecutiveMisses=0.
 *  — Если (miss):
 *       • Помечаем board[r][c] = MISS, increment consecutiveMisses++.
 *
 * Приоритет ходов: **hunt-режим > диагональная фаза > probability-режим**.
 */
class DiagonalProbabilityStrategy : BaseShootingStrategy() {

    /** Состояние клетки на «виртуальном» поле. */
    private enum class CellState { EMPTY, MISS, HIT, SUNK }

    /** Виртуальное поле (10×10) для хранения статусов: EMPTY/MISS/HIT/SUNK. */
    private val board = Array(SIZE) { Array(SIZE) { CellState.EMPTY } }

    /** Длины тех кораблей, что ещё не потоплены. Копия INITIAL_SHIPS. */
    private val remainingShips = INITIAL_SHIPS.toMutableList()

    /** Счётчик подряд идущих промахов (для выхода из диагональной фазы). */
    private var consecutiveMisses = 0

    /** Флаг: мы ещё в диагональной фазе? */
    private var diagonalPhase = true

    private val diagonalOrder = listOf(0, 9, 1, 8, 2, 7, 3, 6, 4, 5)
    /** Порядок точек «главной» диагонали. */
    private val mainShots: List<Pair<Int, Int>> =
        diagonalOrder.map { it to it }

    /** Порядок точек «побочной» диагонали. */
    private val secondaryShots: List<Pair<Int, Int>> =
        diagonalOrder.map { it to (SIZE - 1 - it) }

    private var mainIndex = 0         // текущий индекс в mainShots
    private var secondaryIndex = 0    // текущий индекс в secondaryShots
    private var useMain = true        // true→ бьём по mainShots; false→ по secondaryShots

    // ================================================================
    // 1) computeNextShot() — финальный метод выбора хода
    // ================================================================
    override fun computeNextShot(): Pair<Int, Int> {
        // ——— 1. Hunt-режим (добивание) ———
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
            // Если все кандидаты устарели → сбрасываем очередь
            huntQueue.clear()
        }

        // ——— 2. Диагональная фаза ———
        val largest = remainingShips.maxOrNull() ?: 1
        val dynamicThreshold = when (largest) {
            4 -> 14
            3 -> 12
            2 -> 8
            else -> 6
        }

        if (diagonalPhase && consecutiveMisses < dynamicThreshold) {
            if (useMain) {
                // 2.1) Бьём по mainShots
                while (mainIndex < mainShots.size) {
                    val (r, c) = mainShots[mainIndex++]
                    if (board[r][c] == CellState.EMPTY && !hasTried(r, c)) {
                        return r to c
                    }
                }
                // Если mainShots исчерпаны → переключаемся на secondaryShots
                useMain = false
                return computeNextShot()
            } else {
                // 2.2) Бьём по secondaryShots
                while (secondaryIndex < secondaryShots.size) {
                    val (r, c) = secondaryShots[secondaryIndex++]
                    if (board[r][c] == CellState.EMPTY && !hasTried(r, c)) {
                        return r to c
                    }
                }
                // Обе диагонали исчерпаны → конец диагональной фазы
                diagonalPhase = false
            }
        } else {
            // Либо промахов подряд стало слишком много, либо диагонали кончились
            diagonalPhase = false
        }

        // ——— 3. Probability Mode ———
        return computeProbabilityShot()
    }

    // ================================================================
    // 2) onShotResult() — обновление после хода
    // ================================================================
    override fun onShotResult(lastShot: Pair<Int, Int>?, hit: Boolean, sunk: Boolean) {
        if (lastShot == null) return
        val (r, c) = lastShot

        when {
            // —— 1) Попадание + сразу потопили корабль ——
            hit && sunk -> {
                // [a] Добавляем (r,c) в huntHits, если его там ещё нет
                if ((r to c) !in huntHits) {
                    huntHits.add(r to c)
                }
                // [b] Определяем длину потопленного корабля
                val justSunkLen = remainingShips.maxOrNull() ?: 1
                // [c] Ищем цепочку hit-точек нужной длины
                val chain = extractSunkChain(huntHits, justSunkLen) ?: listOf(r to c)
                // [d] Помечаем chain как SUNK + строим буфер MISS
                markBufferAround(chain)
                // [e] Удаляем потопленную длину из remainingShips
                remainingShips.remove(justSunkLen)
                // [f] Сбрасываем hunt-режим
                resetHuntMode()
                // [g] Сбрасываем consecutiveMisses
                consecutiveMisses = 0
            }

            // —— 2) Просто попадание (hit но не sunk) ——
            hit -> {
                board[r][c] = CellState.HIT
                huntHits.add(r to c)
                enqueueBasedOnHits()
                consecutiveMisses = 0
            }

            // —— 3) Промах ——
            else -> {
                board[r][c] = CellState.MISS
                consecutiveMisses++
            }
        }
    }

    // ================================================================
    // 3) Probability Mode: строим «тепловую карту» по оставшимся кораблям
    // ================================================================
    private fun computeProbabilityShot(): Pair<Int, Int> {
        // [1] Инициализируем массив весов
        val counts = Array(SIZE) { IntArray(SIZE) }

        // [2] Перебираем каждый корабль длины length
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

        // [3] Ищем максимум среди EMPTY-клеток
        var maxCount = 0
        for (r in 0 until SIZE) {
            for (c in 0 until SIZE) {
                if (board[r][c] == CellState.EMPTY && counts[r][c] > maxCount) {
                    maxCount = counts[r][c]
                }
            }
        }

        // [4] Составляем список кандидатов с counts == maxCount
        val candidates = mutableListOf<Pair<Int, Int>>()
        for (r in 0 until SIZE) {
            for (c in 0 until SIZE) {
                if (board[r][c] == CellState.EMPTY && counts[r][c] == maxCount) {
                    candidates.add(r to c)
                }
            }
        }

        // [5] Если есть кандидаты — выбираем случайного
        if (candidates.isNotEmpty()) {
            return candidates.random(Random.Default)
        }
        // [6] Иначе (крайний случай) — возвращаем первую свободную EMPTY
        return findFirstEmptyCell() ?: throw IllegalStateException("No empty cell left")
    }

    // ================================================================
    // 4) Находит первую EMPTY ячейку, по которой ещё не стреляли
    // ================================================================
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

    // ================================================================
    // 5) Ищем цепочку из hit-точек длины length
    // ================================================================
    private fun extractSunkChain(
        hits: List<Pair<Int, Int>>,
        length: Int
    ): List<Pair<Int, Int>>? {
        if (hits.size < length) return null

        // Проверка горизонтальных цепочек
        val byRow = hits.groupBy { it.first }
        for ((row, pts) in byRow) {
            if (pts.size >= length) {
                val cols = pts.map { it.second }.sorted()
                for (i in 0..(cols.size - length)) {
                    val slice = cols.subList(i, i + length)
                    if (slice.last() - slice.first() == length - 1) {
                        return slice.map { row to it }
                    }
                }
            }
        }

        // Проверка вертикальных цепочек
        val byCol = hits.groupBy { it.second }
        for ((col, pts) in byCol) {
            if (pts.size >= length) {
                val rows = pts.map { it.first }.sorted()
                for (i in 0..(rows.size - length)) {
                    val slice = rows.subList(i, i + length)
                    if (slice.last() - slice.first() == length - 1) {
                        return slice.map { it to col }
                    }
                }
            }
        }

        return null
    }

    // ================================================================
    // 6) Помечаем hits как SUNK и строим вокруг них буфер MISS
    // ================================================================
    private fun markBufferAround(hits: List<Pair<Int, Int>>) {
        if (hits.isEmpty()) return

        // 6.1) Помечаем сами hits как SUNK
        for ((r, c) in hits) {
            board[r][c] = CellState.SUNK
        }

        // 6.2) Вокруг каждой точки помечаем буфер (8 соседних клеток) как MISS (если они были EMPTY)
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
