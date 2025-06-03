package com.example.battleship_game.strategies.shooting

/**
 * Базовая абстракция для любой стратегии стрельбы.
 *
 * Основная ответственность:
 *  - Хранить матрицу tried[row][col]: true – если клетка уже обстреляна.
 *  - Проверять валидность координат (0..9).
 *  - Предоставлять общие «hunt-helper-методы» для добивания кораблей.
 *  - Определять контракт getNextShot() → computeNextShot() → setShotResult().
 *
 * Наследники должны реализовать:
 *  - computeNextShot()  – возвращает (row, col) для следующего выстрела (без учёта tried[][]).
 *  - onShotResult(...)  – получает результат (hit/sunk) последнего выстрела и обновляет своё внутреннее состояние.
 *
 * Кроме того, в базовом классе вынесены:
 *  - huntQueue и huntHits         — очередь и список попаданий для режима «добивания».
 *  - enqueueOrthogonal(...)       — добавляет 4 ортогональных соседа.
 *  - enqueueBasedOnHits(...)      — строит очередь добивания в зависимости от накопленных попаданий.
 *  - resetHuntMode()              — сброс всех внутренних данных добивания.
 *  - hasTried(...)                — проверка, было ли уже стрельба по клетке.
 */
abstract class BaseShootingStrategy : ShootingStrategy {
    // ——————————————————————————————————————————————————————————————————————————————
    // a) «Известные» уже обстрелянные клетки
    // ——————————————————————————————————————————————————————————————————————————————
    private val tried = Array(10) { BooleanArray(10) { false } }

    // Последняя выбранная стратегия выстрела (row, col)
    private var lastShot: Pair<Int, Int>? = null

    // ——————————————————————————————————————————————————————————————————————————————
    // b) «Hunt-Mode» (добивание) — общие поля для всех стратегий
    // ——————————————————————————————————————————————————————————————————————————————
    /** Очередь клеток для «добивания» (после первого попадания). */
    protected val huntQueue = ArrayDeque<Pair<Int, Int>>()

    /**
     * Список уже попадённых точек одного конкретного корабля,
     * которые удобнее аккумулировать, чтобы потом строить очередь добивания.
     */
    protected val huntHits = mutableListOf<Pair<Int, Int>>()

    // ——————————————————————————————————————————————————————————————————————————————
    // 1. getNextShot(): финальный метод, ничего не переопределяется.
    // ——————————————————————————————————————————————————————————————————————————————
    final override fun getNextShot(): Pair<Int, Int> {
        // Пытаемся до 100 раз вызвать computeNextShot, чтобы получить «чистую» и ещё не tried клетку
        repeat(100) {
            val (r, c) = computeNextShot()
            if (isValidCell(r, c) && !tried[r][c]) {
                lastShot = r to c
                return r to c
            }
        }
        // Если что-то пошло не так — возвращаем первую свободную «в лоб»
        for (r in 0 until 10) {
            for (c in 0 until 10) {
                if (!tried[r][c]) {
                    lastShot = r to c
                    return r to c
                }
            }
        }
        throw IllegalStateException("No available cells to shoot")
    }

    // ——————————————————————————————————————————————————————————————————————————————
    // 2. setShotResult(): финальный метод, помечает tried и передаёт управление в onShotResult().
    // ——————————————————————————————————————————————————————————————————————————————
    final override fun setShotResult(hit: Boolean, sunk: Boolean) {
        lastShot?.let { (r, c) ->
            if (isValidCell(r, c)) {
                tried[r][c] = true
            }
        }
        onShotResult(lastShot, hit, sunk)
    }

    // ——————————————————————————————————————————————————————————————————————————————
    // 3. Вспомогательные методы для стратегий
    // ——————————————————————————————————————————————————————————————————————————————

    /**
     * Проверяет, что координаты (row, col) лежат в диапазоне 0..9.
     */
    protected fun isValidCell(row: Int, col: Int): Boolean {
        return row in 0..9 && col in 0..9
    }

    /**
     * Проверка, был ли уже обстрел по клетке (row, col).
     * (Позволяет наследникам проверять tried[][] напрямую.)
     */
    protected fun hasTried(row: Int, col: Int): Boolean {
        return tried[row][col]
    }

    /**
     * «Сырая» логика выбора следующей клетки (row, col), без учёта tried[].
     * Должна быть реализована в наследнике.
     */
    protected abstract fun computeNextShot(): Pair<Int, Int>

    /**
     * Обработка результата последнего выстрела. Наследник тут обновляет своё internal-state.
     *
     * @param lastShot – координаты (row, col) последнего выстрела (или null, если ещё не было выстрела).
     * @param hit      – true, если этот ход попал в корабль.
     * @param sunk     – true, если этот ход потопил корабль целиком.
     */
    protected open fun onShotResult(
        lastShot: Pair<Int, Int>?,
        hit: Boolean,
        sunk: Boolean
    ) {
        // По умолчанию ничего не делаем. В конкретных стратегиях можно переопределить.
    }

    // ——————————————————————————————————————————————————————————————————————————————
    // 4. Hunt-helpers: методы добивания
    // ——————————————————————————————————————————————————————————————————————————————

    /**
     * Добавляет в очередь добивания все 4 ортогональных соседа клетки (r,c):
     * (r,c+1), (r,c-1), (r+1,c), (r-1,c). При этом проверяем, что они валидны
     * и ещё не обстреляны (huntHits) и не стоят уже в queue.
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
     * Основная логика построения очереди «добивания» (huntQueue), когда в huntHits накоплено
     * некоторое количество попаданий одного корабля.
     *
     * Правила:
     *  - Если в huntHits ровно 1 точка → ставим 4 её ортогональных соседа.
     *  - Если >=2 точек и они лежат в одной строке → enqueue ровно две точки: «слева» от
     *    минимальной палубы и «справа» от максимальной палубы.
     *  - Если >=2 точек и они лежат в одном столбце → enqueue ровно две точки: «сверху» от
     *    минимальной палубы и «снизу» от максимальной палубы.
     *  - Если пока попаданий >=2, но они НЕ лежат в одной строке или не лежат в одном столбце
     *    (т. е. два случайных «разрозненных» попадания) → просто enqueue ортососеди от последнего попадания.
     */
    protected open fun enqueueBasedOnHits() {
        // Очищаем предыдущую очередь
        huntQueue.clear()

        if (huntHits.size == 1) {
            // Только одна точка – ставим её 4 ортососеда
            val (r, c) = huntHits.first()
            enqueueOrthogonal(r, c)
            return
        }

        // Проверяем, лежат ли все попадания в одной строке
        val sameRow = huntHits.map { it.first }.distinct().size == 1
        // Проверяем, лежат ли все попадания в одном столбце
        val sameCol = huntHits.map { it.second }.distinct().size == 1

        if (sameRow) {
            // Горизонтальный корабль: берём minCol и maxCol
            val row = huntHits.first().first
            val sortedCols = huntHits.map { it.second }.sorted()
            val leftC = sortedCols.first()
            val rightC = sortedCols.last()

            // Клетка слева (row,leftC-1), если ещё не tried
            val leftCell = row to (leftC - 1)
            if (isValidCell(leftCell.first, leftCell.second) && !hasTried(leftCell.first, leftCell.second)) {
                huntQueue.addLast(leftCell)
            }
            // Клетка справа (row,rightC+1)
            val rightCell = row to (rightC + 1)
            if (isValidCell(rightCell.first, rightCell.second) && !hasTried(rightCell.first, rightCell.second)) {
                huntQueue.addLast(rightCell)
            }
        } else if (sameCol) {
            // Вертикальный корабль: берём minRow и maxRow
            val col = huntHits.first().second
            val sortedRows = huntHits.map { it.first }.sorted()
            val topR = sortedRows.first()
            val bottomR = sortedRows.last()

            // Клетка сверху (topR-1,col)
            val upCell = (topR - 1) to col
            if (isValidCell(upCell.first, upCell.second) && !hasTried(upCell.first, upCell.second)) {
                huntQueue.addLast(upCell)
            }
            // Клетка снизу (bottomR+1,col)
            val downCell = (bottomR + 1) to col
            if (isValidCell(downCell.first, downCell.second) && !hasTried(downCell.first, downCell.second)) {
                huntQueue.addLast(downCell)
            }
        } else {
            // У нас >=2 попаданий, но они не в одну линию → enqueue ортососеди от последнего попадания
            val (r, c) = huntHits.last()
            enqueueOrthogonal(r, c)
        }
    }

    /**
     * Полный сброс режима «добивания»:
     * очищаем queue, очищаем накопленные попадания.
     */
    protected fun resetHuntMode() {
        huntQueue.clear()
        huntHits.clear()
    }
}
