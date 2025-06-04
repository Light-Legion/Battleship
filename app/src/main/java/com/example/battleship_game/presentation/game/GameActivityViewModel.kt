package com.example.battleship_game.presentation.game

import androidx.lifecycle.ViewModel
import com.example.battleship_game.data.model.Difficulty
import com.example.battleship_game.data.model.ShipPlacement
import com.example.battleship_game.strategies.shooting.BaseShootingStrategy.Companion.SIZE
import com.example.battleship_game.strategies.shooting.DensityAnalysisStrategy
import com.example.battleship_game.strategies.shooting.ShootingStrategy
import kotlinx.coroutines.delay

/**
 * ViewModel для экрана боя «Морской бой».
 *
 * Обязанности:
 * 1) Хранить внутренние «сетки» (10×10) для игрока и компьютера:
 *    в каждой клетке либо 0 (пусто), либо shipId (>0), либо –shipId (попадание), либо –99 (промах).
 * 2) Хранить «сколько палуб осталось» (remaining decks) для каждого корабля (по shipId).
 * 3) Обрабатывать выстрел игрока (processPlayerShot) и возвращать результат (Hit/Miss/Sunk + буфер).
 * 4) Асинхронно (с задержкой 3 с) обрабатывать выстрел компьютера (processComputerShot) через выбранную стратегию.
 * 5) Отмечать, завершён ли бой (isBattleOver), и переключать флаг isPlayerTurn.
 */
class GameActivityViewModel : ViewModel() {

    // ──────────────── 1. Входные данные (инициализируются извне) ────────────────

    lateinit var playerShips: List<ShipPlacement>
    lateinit var computerShips: List<ShipPlacement>
    lateinit var difficulty: Difficulty
    lateinit var shootingStrategy: ShootingStrategy

    // ──────────────── 2. Внутренние “сетки” 10×10 ────────────────
    // > 0 = shipId, < 0 && != CELL_MISS → повреждённая палуба, CELL_MISS = −99 (промах)
    private val playerGrid = Array(10) { IntArray(10) }
    private val computerGrid = Array(10) { IntArray(10) }

    // ──────────────── 3. “Остаточные палубы” (по shipId) ────────────────
    private val playerRemainingDecks   = mutableMapOf<Int, Int>()
    private val computerRemainingDecks = mutableMapOf<Int, Int>()

    // ──────────────── 4. Публичные флаги для Activity ────────────────
    /** true = ход игрока; false = ход компьютера */
    var isPlayerTurn: Boolean = true

    /** true = все корабли одной из сторон уже потоплены */
    var isBattleOver: Boolean = false

    // ──────────────── 5. Константа “промах” ────────────────
    private companion object {
        const val CELL_MISS = -99
    }

    // ──────────────── 6. Инициализация “битва готова” ────────────────

    /**
     * Заполняем playerGrid и computerGrid по спискам playerShips / computerShips,
     * проставляем remainingDecks, сбрасываем флаги.
     */
    fun initBattle() {

        // 6.1) Игрок
        playerShips.forEach { ship ->
            playerRemainingDecks[ship.shipId] = ship.length
            repeat(ship.length) { i ->
                val r = ship.startRow + if (ship.isVertical) i else 0
                val c = ship.startCol + if (ship.isVertical) 0 else i
                if (r in 0..9 && c in 0..9) {
                    playerGrid[r][c] = ship.shipId
                }
            }
        }
        // 6.2) Компьютер
        computerShips.forEach { ship ->
            computerRemainingDecks[ship.shipId] = ship.length
            repeat(ship.length) { i ->
                val r = ship.startRow + if (ship.isVertical) i else 0
                val c = ship.startCol + if (ship.isVertical) 0 else i
                if (r in 0..9 && c in 0..9) {
                    computerGrid[r][c] = ship.shipId
                }
            }
        }
        // 6.3) Сброс флагов
        isPlayerTurn = true
        isBattleOver  = false
    }

    // ───────────────────────────────────────────────────────────────────
    // 7. public fun playerShot(row, col): ShotResult
    // ───────────────────────────────────────────────────────────────────

    /**
     * Игрок стреляет (row, col) в поле компьютера.
     * @return ShotResult(hit=…, sunk=…, sunkShip?, bufferCells?).
     */
    fun playerShot(row: Int, col: Int): ShotResult {
        if (isBattleOver) {
            // бой уже завершён — просто возвращаем “ничего не делаем”
            return ShotResult(row, col, hit = false, sunk = false)
        }

        val cellValue = computerGrid[row][col]
        return if (cellValue > 0) {
            // 1) Попал в невредимую палубу
            processHit(
                row = row, col = col,
                grid = computerGrid,
                remainingDecks = computerRemainingDecks,
                ships = computerShips,
                onSunk = { sunkShip ->
                    // Если при этом был потоплен последний корабль компьютера
                    if (computerRemainingDecks.values.all { it == 0 }) {
                        isBattleOver = true
                    }
                },
                informStrategy = false
            )
        } else {
            // 2) Промах (или корабль уже был поражён здесь)
            processMiss(
                row = row, col = col,
                grid = computerGrid,
                informStrategy = false
            ).also {
                // ход перешёл компьютеру
                isPlayerTurn = false
            }
        }
    }

    // ───────────────────────────────────────────────────────────────────
    // 8. suspend fun computerShot(): ShotResult
    // ───────────────────────────────────────────────────────────────────

    /**
     * Компьютер «думает» 3 секунды, потом стреляет (row,col) по полю игрока,
     * используя shootingStrategy.getNextShot(). Возвращает ShotResult.
     */
    suspend fun computerShot(): ShotResult {
        if (isBattleOver) {
            return ShotResult(0, 0, hit = false, sunk = false)
        }

        // 8.1) Подождать 3 секунды
        delay(3000L)

        // 8.2) Получить (row, col) из стратегии (строго в формате (row, col))
        val (row, col) = shootingStrategy.getNextShot()

        // 8.3) Выполнить «выстрел» по playerGrid
        val cellValue = playerGrid[row][col]
        return if (cellValue > 0) {
            // Компьютер попал в палубу игрока
            processHit(
                row = row, col = col,
                grid = playerGrid,
                remainingDecks = playerRemainingDecks,
                ships = playerShips,
                onSunk = { sunkShip ->
                    // Если это был последний корабль игрока
                    if (playerRemainingDecks.values.all { it == 0 }) {
                        isBattleOver = true
                    }
                },
                informStrategy = true
            )
        } else {
            // Компьютер промахнулся или уже там стрелял
            processMiss(
                row = row, col = col,
                grid = playerGrid,
                informStrategy = true
            ).also {
                // Передаём ход игроку
                isPlayerTurn = true
            }
        }
    }

    // ───────────────────────────────────────────────────────────────────
    // 9. processHit(...) + processMiss(...) (единожды подтверждают стратегию)
    // ───────────────────────────────────────────────────────────────────

    /**
     * Обработка «попал» в grid[row][col] (корабль ещё не разрушен) + возможное «упало до 0» (potopil):
     *  - Помечает grid[row][col] = −shipId
     *  - Уменьшает remainingDecks[shipId]
     *  - Если informStrategy == true, то сообщает стратегии ровно один вызов:
     *      * если потопил = true → setShotResult(hit=true, sunk=true)
     *      * иначе                  → setShotResult(hit=true, sunk=false)
     *  - Если потоплен корабль целиком → вычисляет bufferCells и помечает все buffer (–99)
     *      * вызывает onSunk(sunkShip)  (например, установить флаг isBattleOver)
     *  - Возвращает соответствующий ShotResult
     */
    private fun processHit(
        row: Int,
        col: Int,
        grid: Array<IntArray>,
        remainingDecks: MutableMap<Int, Int>,
        ships: List<ShipPlacement>,
        onSunk: (ShipPlacement) -> Unit,
        informStrategy: Boolean
    ): ShotResult {
        // 1) Берём shipId, помечаем cell = −shipId:
        val shipId = grid[row][col]
        grid[row][col] = -shipId

        // 2) Уменьшаем “палубы” для этого корабля:
        remainingDecks[shipId] = remainingDecks[shipId]!! - 1

        val justSunk = (remainingDecks[shipId] == 0)

        // 3) Сообщаем стратегии **ровно один раз**:
        if (informStrategy) {
            if (justSunk) {
                shootingStrategy.setShotResult(hit = true, sunk = true)
            } else {
                shootingStrategy.setShotResult(hit = true, sunk = false)
            }
        }

        return if (justSunk) {
            // 4) Найдём сам ShipPlacement, чтобы построить буфер:
            val sunkShip = ships.first { it.shipId == shipId }

            // 5) Вычисляем «буферные клетки»:
            val buffer = computeBufferCells(sunkShip)

            // 6) Помечаем буфер как CELL_MISS (если там ещё была “0”):
            buffer.forEach { (r, c) ->
                if (grid[r][c] >= 0) {
                    grid[r][c] = CELL_MISS
                }
            }

            // 7) Уведомляем, что именно этот ShipPlacement был потоплен:
            onSunk(sunkShip)

            // 8) Возвращаем полный результат:
            ShotResult(
                row = row,
                col = col,
                hit = true,
                sunk = true,
                sunkShip = sunkShip,
                bufferCells = buffer
            )
        } else {
            // 4) Просто попал, но корабль ещё жив
            ShotResult(row = row, col = col, hit = true, sunk = false)
        }
    }

    /**
     * Обработка «промаха» grid[row][col] (если там “0” → ставим −99), и если informStrategy=true,
     * то вызываем shootingStrategy.setShotResult(hit = false, sunk = false).
     */
    private fun processMiss(
        row: Int,
        col: Int,
        grid: Array<IntArray>,
        informStrategy: Boolean
    ): ShotResult {
        if (grid[row][col] == 0) {
            grid[row][col] = CELL_MISS
        }
        if (informStrategy) {
            shootingStrategy.setShotResult(hit = false, sunk = false)
        }
        return ShotResult(row = row, col = col, hit = false, sunk = false)
    }

    // ───────────────────────────────────────────────────────────────────
    // 10. computeBufferCells(...) как было (никаких изменений) ────────────────
    // ───────────────────────────────────────────────────────────────────

    /**
     * Возвращает список координат (r,c) вокруг ship (одна клетка во все стороны),
     * которые НЕ входят в сам корабль (его палубы). Эти клетки станут “промахами” (buffer).
     */
    fun computeBufferCells(ship: ShipPlacement): List<Pair<Int, Int>> {
        val buffer = mutableListOf<Pair<Int, Int>>()
        for (i in -1..ship.length) {
            for (j in -1..1) {
                val r = ship.startRow + if (ship.isVertical) i else j
                val c = ship.startCol + if (ship.isVertical) j else i
                if (r in 0..9 && c in 0..9) {
                    val onShip = (i in 0 until ship.length && j == 0)
                    if (!onShip) {
                        buffer.add(r to c)
                    }
                }
            }
        }
        return buffer
    }

    /**
     * Возвращает список координат ВСЕХ пока не потопленных палуб игрока.
     * То есть перебираем playerShips, но для каждого shipId проверяем
     * playerRemainingDecks[shipId] > 0.
     */
    fun getAllLivePlayerDecks(): List<Pair<Int, Int>> {
        val liveDecks = mutableListOf<Pair<Int, Int>>()
        for (ship in playerShips) {
            val id = ship.shipId
            // пропускаем, если уже весь корабль потоплен
            if (playerRemainingDecks[id] == 0) continue
            // иначе добавляем все палубы:
            repeat(ship.length) { i ->
                val r = ship.startRow + if (ship.isVertical) i else 0
                val c = ship.startCol + if (ship.isVertical) 0 else i
                liveDecks.add(r to c)
            }
        }
        return liveDecks
    }
}