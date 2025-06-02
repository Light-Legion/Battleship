package com.example.battleship_game.presentation.game

import androidx.lifecycle.ViewModel
import com.example.battleship_game.data.model.Difficulty
import com.example.battleship_game.data.model.ShipPlacement
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

    // ────────────────────────────────────────────────────────────────
    // 1. Входные данные (инициализируются извне перед initBattle)
    // ────────────────────────────────────────────────────────────────

    lateinit var playerShips: List<ShipPlacement>
    lateinit var computerShips: List<ShipPlacement>
    lateinit var difficulty: Difficulty
    lateinit var shootingStrategy: ShootingStrategy

    // ────────────────────────────────────────────────────────────────
    // 2. Внутренние «сетки» 10×10:
    //    >0 = shipId, <0 && != CELL_MISS → повреждённая палуба, CELL_MISS = промах
    // ────────────────────────────────────────────────────────────────

    private val playerGrid = Array(10) { IntArray(10) }
    private val computerGrid = Array(10) { IntArray(10) }

    // ────────────────────────────────────────────────────────────────
    // 3. Счётчики «сколько палуб осталось» (key = shipId)
    // ────────────────────────────────────────────────────────────────

    private val playerRemainingDecks = mutableMapOf<Int, Int>()
    private val computerRemainingDecks = mutableMapOf<Int, Int>()

    // ────────────────────────────────────────────────────────────────
    // 4. Публичные флаги (для Activity)
    // ────────────────────────────────────────────────────────────────

    /** true = сейчас ход игрока; false = ход компьютера */
    var isPlayerTurn: Boolean = true

    /** true = все корабли одной из сторон потоплены */
    var isBattleOver: Boolean = false

    // ────────────────────────────────────────────────────────────────
    // 5. Константа вместо «магического» –99
    // ────────────────────────────────────────────────────────────────

    private companion object {
        /** Специальное значение в grid, означающее «здесь был промах» */
        const val CELL_MISS = -99
    }

    // ────────────────────────────────────────────────────────────────
    // 6. Инициализация перед началом боя
    // ────────────────────────────────────────────────────────────────

    fun initBattle() {
        // 6.1) Расставляем корабли игрока на playerGrid
        playerShips.forEach { ship ->
            playerRemainingDecks[ship.shipId] = ship.length
            for (i in 0 until ship.length) {
                val r = ship.startRow + if (ship.isVertical) i else 0
                val c = ship.startCol + if (ship.isVertical) 0 else i
                if (r in 0..9 && c in 0..9) {
                    playerGrid[r][c] = ship.shipId
                }
            }
        }
        // 6.2) Расставляем корабли компьютера на computerGrid
        computerShips.forEach { ship ->
            computerRemainingDecks[ship.shipId] = ship.length
            for (i in 0 until ship.length) {
                val r = ship.startRow + if (ship.isVertical) i else 0
                val c = ship.startCol + if (ship.isVertical) 0 else i
                if (r in 0..9 && c in 0..9) {
                    computerGrid[r][c] = ship.shipId
                }
            }
        }
        // 6.3) Сбрасываем флаги
        isPlayerTurn = true
        isBattleOver = false
    }

    // ────────────────────────────────────────────────────────────────
    // 7. Обработка выстрела игрока по полю компьютера
    // ────────────────────────────────────────────────────────────────

    /**
     * Игрок стреляет в клетку (row, col) на поле компьютера.
     * Возвращает результат (попал/промах, потопил ли корабль, буферные клетки).
     */
    fun playerShot(row: Int, col: Int): ShotResult {
        if (isBattleOver) {
            return ShotResult(row, col, hit = false, sunk = false)
        }
        val cellValue = computerGrid[row][col]
        return if (cellValue > 0) {
            // Попал в живую палубу
            processHit(
                row = row,
                col = col,
                grid = computerGrid,
                remainingDecks = computerRemainingDecks,
                ships = computerShips,
                onSunk = { sunkShip ->
                    // Если весь флот компьютера потоплен
                    if (computerRemainingDecks.values.all { it == 0 }) {
                        isBattleOver = true
                    }
                }
            )
        } else {
            // Промах или уже стреляли
            processMiss(
                row = row,
                col = col,
                grid = computerGrid
            ).also {
                // Передаём ход компьютеру
                isPlayerTurn = false
            }
        }
    }

    // ────────────────────────────────────────────────────────────────
    // 8. Асинхронная обработка хода компьютера (задержка 3 секунды)
    // ────────────────────────────────────────────────────────────────

    /**
     * Компьютер «думает» 3 секунды, затем стреляет в поле игрока.
     * Возвращает ShotResult (row, col, hit, sunk, sunkShip, bufferCells).
     */
    suspend fun computerShot(): ShotResult {
        if (isBattleOver) {
            return ShotResult(0, 0, hit = false, sunk = false)
        }
        delay(3000) // симуляция «обдумывания»

        // Получаем следующую клетку из стратегии
        val (col, row) = shootingStrategy.getNextShot()

        val cellValue = playerGrid[row][col]
        return if (cellValue > 0) {
            // Компьютер попал в живую палубу игрока
            processHit(
                row = row,
                col = col,
                grid = playerGrid,
                remainingDecks = playerRemainingDecks,
                ships = playerShips,
                onSunk = { sunkShip ->
                    // Если весь флот игрока потоплен
                    if (playerRemainingDecks.values.all { it == 0 }) {
                        isBattleOver = true
                    }
                },
                informStrategy = true
            )
        } else {
            // Компьютер промах или уже стрелял
            processMiss(
                row = row,
                col = col,
                grid = playerGrid,
                informStrategy = true
            ).also {
                // Передаём ход игроку
                isPlayerTurn = true
            }
        }
    }

    // ────────────────────────────────────────────────────────────────
    // 9. Приватная утилита: обработка попадания
    // ────────────────────────────────────────────────────────────────

    /**
     * Ставит в grid[row][col] = –shipId, уменьшает remainingDecks[shipId].
     * Если потоплен весь корабль, вычисляет bufferCells и вызывает onSunk.
     * Если informStrategy=true, то сообщает стратегии результат (hit=true, sunk=?).
     */
    private fun processHit(
        row: Int,
        col: Int,
        grid: Array<IntArray>,
        remainingDecks: MutableMap<Int, Int>,
        ships: List<ShipPlacement>,
        onSunk: (ShipPlacement) -> Unit,
        informStrategy: Boolean = false
    ): ShotResult {
        val shipId = grid[row][col]
        grid[row][col] = -shipId // помечаем палубу как повреждённую
        remainingDecks[shipId] = remainingDecks[shipId]!! - 1

        if (informStrategy) {
            shootingStrategy.setShotResult(hit = true, sunk = false)
        }

        return if (remainingDecks[shipId] == 0) {
            // Корабль полностью потонул
            val sunkShip = ships.first { it.shipId == shipId }
            val buffer = computeBufferCells(sunkShip)
            buffer.forEach { (r, c) ->
                if (grid[r][c] >= 0) {
                    grid[r][c] = CELL_MISS
                }
            }
            if (informStrategy) {
                shootingStrategy.setShotResult(hit = true, sunk = true)
            }
            onSunk(sunkShip)
            ShotResult(row = row, col = col, hit = true, sunk = true, sunkShip = sunkShip, bufferCells = buffer)
        } else {
            // Попал, но не потопил
            ShotResult(row = row, col = col, hit = true, sunk = false)
        }
    }

    // ────────────────────────────────────────────────────────────────
    // 10. Приватная утилита: обработка промаха
    // ────────────────────────────────────────────────────────────────

    /**
     * Помечает grid[row][col] = CELL_MISS (если там 0), информирует стратегию (если нужно),
     * возвращает ShotResult(hit=false).
     */
    private fun processMiss(
        row: Int,
        col: Int,
        grid: Array<IntArray>,
        informStrategy: Boolean = false
    ): ShotResult {
        if (grid[row][col] == 0) {
            grid[row][col] = CELL_MISS
        }
        if (informStrategy) {
            shootingStrategy.setShotResult(hit = false, sunk = false)
        }
        return ShotResult(row = row, col = col, hit = false, sunk = false)
    }

    // ────────────────────────────────────────────────────────────────
    // 11. Приватная утилита: вычисление «буферных» клеток
    // ────────────────────────────────────────────────────────────────

    /**
     * Возвращает список координат (r,c) вокруг ship (одна клетка во все стороны),
     * которые не входят в сам корабль (это «буфер»).
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
}