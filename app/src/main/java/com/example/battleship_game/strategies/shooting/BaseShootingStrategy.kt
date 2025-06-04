package com.example.battleship_game.strategies.shooting

/**
 * Базовая абстракция для любой стратегии стрельбы.
 *
 * Отвечает за:
 *  1) Хранение матрицы tried[row][col]: true — если клетка уже обстреляна.
 *  2) Проверку валидности координат (0..SIZE-1).
 *  3) Предоставление общих «hunt-helper-методов» (добивание).
 *  4) Определение контракта getNextShot() → computeNextShot() → setShotResult().
 *
 * Наследники должны реализовать:
 *  - computeNextShot() — возвращает (row, col) для следующего выстрела без учёта tried[].
 *  - onShotResult(...) — получает результат (hit/sunk) последнего выстрела и обновляет внутреннее состояние.
 */
abstract class BaseShootingStrategy : ShootingStrategy {

    companion object {
        /** Размер поля (N×N). */
        const val SIZE = 10

        /** Изначальный набор длин кораблей (для всех стратегий). */
        val INITIAL_SHIPS = listOf(4, 3, 3, 2, 2, 2, 1, 1, 1, 1)
    }

    // ===============================================================================
    // 1) «Известные» уже обстрелянные клетки
    // ===============================================================================
    private val tried = Array(SIZE) { BooleanArray(SIZE) { false } }
    private var lastShot: Pair<Int, Int>? = null

    // ===============================================================================
    // 2) «Hunt-Mode» (добивание) — общие поля для всех стратегий
    // ===============================================================================
    /** Очередь клеток для «добивания» (после попадания). */
    protected val huntQueue = ArrayDeque<Pair<Int, Int>>()

    /**
     * Список уже попадённых точек одного конкретного корабля,
     * нужный для построения очереди добивания.
     */
    protected val huntHits = mutableListOf<Pair<Int, Int>>()

    // ===============================================================================
    // 3) Финальные методы: getNextShot() и setShotResult()
    // ===============================================================================
    final override fun getNextShot(): Pair<Int, Int> {
        // Пытаемся до 100 раз вызвать computeNextShot(),
        // чтобы получить «чистую» и ещё не tried клетку
        repeat(100) {
            val (r, c) = computeNextShot()
            if (isValidCell(r, c) && !tried[r][c]) {
                lastShot = r to c
                return r to c
            }
        }
        // Если не удалось за 100 попыток — вернуть первую свободную «в лоб»
        for (r in 0 until SIZE) {
            for (c in 0 until SIZE) {
                if (!tried[r][c]) {
                    lastShot = r to c
                    return r to c
                }
            }
        }
        throw IllegalStateException("No available cells to shoot")
    }

    final override fun setShotResult(hit: Boolean, sunk: Boolean) {
        // Помечаем tried[row][col] = true
        lastShot?.let { (r, c) ->
            if (isValidCell(r, c)) {
                tried[r][c] = true
            }
        }
        // Даем стратегии возможность обновить своё состояние
        onShotResult(lastShot, hit, sunk)
    }

    // ===============================================================================
    // 4) Вспомогательные методы для наследников
    // ===============================================================================
    /** Проверяет, что координаты (row, col) лежат в диапазоне 0..SIZE-1. */
    protected fun isValidCell(row: Int, col: Int): Boolean {
        return row in 0 until SIZE && col in 0 until SIZE
    }

    /**
     * Проверка, был ли уже выстрел по клетке (row, col).
     * (Позволяет наследникам проверять tried[][] напрямую.)
     */
    protected fun hasTried(row: Int, col: Int): Boolean {
        return tried[row][col]
    }

    /**
     * «Сырая» логика выбора следующей клетки (row, col) без учёта tried[].
     * Должна быть реализована в наследнике.
     */
    protected abstract fun computeNextShot(): Pair<Int, Int>

    /**
     * Обработка результата последнего выстрела. Наследник тут обновляет своё internal-state.
     *
     * @param lastShot — координаты (row, col) последнего выстрела (или null, если ещё не было выстрела).
     * @param hit      — true, если этот ход попал в корабль.
     * @param sunk     — true, если этот ход потопил корабль целиком.
     */
    protected open fun onShotResult(
        lastShot: Pair<Int, Int>?,
        hit: Boolean,
        sunk: Boolean
    ) {
        // По умолчанию ничего не делаем.
    }

    // ===============================================================================
    // 5) Hunt-helpers: методы добивания (универсальные для всех стратегий)
    // ===============================================================================
    /**
     * Добавляет в очередь добивания все 4 ортогональных соседа клетки (row, col):
     * (row, col+1), (row, col-1), (row+1, col), (row-1, col).
     * При этом проверяет, что они:
     *   • валидны (isValidCell),
     *   • ещё не пробиты (!hasTried),
     *   • и не стоят уже в huntQueue.
     */
    protected fun enqueueOrthogonal(row: Int, col: Int) {
        listOf(
            row to (col + 1),
            row to (col - 1),
            (row + 1) to col,
            (row - 1) to col
        ).forEach { cell ->
            val (r, c) = cell
            if (isValidCell(r, c)
                && !hasTried(r, c)
                && cell !in huntQueue
            ) {
                huntQueue.addLast(cell)
            }
        }
    }

    /**
     * Основная логика построения очереди «добивания» (huntQueue), когда в huntHits
     * накоплено некоторое количество попаданий одного корабля.
     *
     * Правила:
     *  — Если в huntHits ровно 1 точка → enqueueOrthogonal() от этой точки.
     *  — Если ≥2 точек и они лежат в одной строке → enqueue ровно две точки:
     *      «слева» от минимальной палубы и «справа» от максимальной палубы.
     *  — Если ≥2 точек и они лежат в одном столбце → enqueue ровно две точки:
     *      «сверху» от минимальной палубы и «снизу» от максимальной палубы.
     *  — Если ≥2 попаданий, но они НЕ лежат в одной строке и НЕ в одной колонке
     *    (т. е. «разрозненные» попадания) → enqueueOrthogonal() от последнего попадания.
     */
    protected open fun enqueueBasedOnHits() {
        // Сброс предыдущей очереди
        huntQueue.clear()

        if (huntHits.size == 1) {
            val (r, c) = huntHits.first()
            enqueueOrthogonal(r, c)
            return
        }

        // Проверяем, все ли попадания в одной строке?
        val sameRow = huntHits.map { it.first }.distinct().size == 1
        // Проверяем, все ли попадания в одном столбце?
        val sameCol = huntHits.map { it.second }.distinct().size == 1

        if (sameRow) {
            // Горизонтальный корабль
            val row = huntHits.first().first
            val sortedCols = huntHits.map { it.second }.sorted()
            val leftC = sortedCols.first()
            val rightC = sortedCols.last()

            // Клетка «слева» от минимальной
            val leftCell = row to (leftC - 1)
            if (isValidCell(leftCell.first, leftCell.second)
                && !hasTried(leftCell.first, leftCell.second)
            ) {
                huntQueue.addLast(leftCell)
            }
            // Клетка «справа» от максимальной
            val rightCell = row to (rightC + 1)
            if (isValidCell(rightCell.first, rightCell.second)
                && !hasTried(rightCell.first, rightCell.second)
            ) {
                huntQueue.addLast(rightCell)
            }
        }
        else if (sameCol) {
            // Вертикальный корабль
            val col = huntHits.first().second
            val sortedRows = huntHits.map { it.first }.sorted()
            val topR = sortedRows.first()
            val bottomR = sortedRows.last()

            // Клетка «сверху»
            val upCell = (topR - 1) to col
            if (isValidCell(upCell.first, upCell.second)
                && !hasTried(upCell.first, upCell.second)
            ) {
                huntQueue.addLast(upCell)
            }
            // Клетка «снизу»
            val downCell = (bottomR + 1) to col
            if (isValidCell(downCell.first, downCell.second)
                && !hasTried(downCell.first, downCell.second)
            ) {
                huntQueue.addLast(downCell)
            }
        }
        else {
            // ≥2 попаданий, но не по одной линии → enqueueOrthogonal() от последнего попадания
            val (r, c) = huntHits.last()
            enqueueOrthogonal(r, c)
        }
    }

    /**
     * Полный сброс «hunt-режима»:
     *  — очищаем huntQueue,
     *  — очищаем huntHits.
     */
    protected fun resetHuntMode() {
        huntQueue.clear()
        huntHits.clear()
    }
}
