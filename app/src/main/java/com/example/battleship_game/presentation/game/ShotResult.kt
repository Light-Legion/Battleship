package com.example.battleship_game.presentation.game

import com.example.battleship_game.data.model.ShipPlacement

/**
 * Результат одного выстрела (для игрока или компьютера).
 *
 * @property row         Строка клетки [0..9]
 * @property col         Столбец клетки [0..9]
 * @property hit         true, если выстрел попал в палубу живого корабля
 * @property sunk        true, если этим выстрелом палуба добила последний оставшийся блок корабля
 * @property sunkShip    если sunk==true → ссылка на описание ShipPlacement этого потопленного корабля
 * @property bufferCells координаты окружающих «буферных» клеток (не на корабле), которые нужно пометить как промах
 */
data class ShotResult(
    val row: Int,
    val col: Int,
    val hit: Boolean,
    val sunk: Boolean,
    val sunkShip: ShipPlacement? = null,
    val bufferCells: List<Pair<Int, Int>> = emptyList()
)
