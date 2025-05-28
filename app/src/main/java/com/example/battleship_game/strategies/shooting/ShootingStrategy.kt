package com.example.battleship_game.strategies.shooting

/**
 * Интерфейс для алгоритмов выбора следующей клетки для выстрела.
 *
 * @method getNextShot возвращает координаты [x, y] следующего выстрела.
 * @method setShotResult сообщает стратегии, попали ли мы (hit) и потопили ли корабль (sunk).
 */
interface ShootingStrategy {
    fun getNextShot(): Pair<Int, Int>
    fun setShotResult(hit: Boolean, sunk: Boolean)
}