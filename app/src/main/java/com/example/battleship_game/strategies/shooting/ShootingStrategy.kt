package com.example.battleship_game.strategies.shooting

/**
 * Интерфейс для алгоритмов выбора следующей клетки для выстрела.
 *
 * Реализации должны предоставлять:
 * - Выбор следующей клетки для выстрела (getNextShot)
 * - Обработку результата выстрела (setShotResult)
 */
interface ShootingStrategy {
    /**
     * Возвращает координаты следующего выстрела (x, y) в диапазоне 0-9.
     * Гарантирует, что клетка еще не обстреливалась.
     */
    fun getNextShot(): Pair<Int, Int>

    /**
     * Обновляет состояние стратегии на основе результата выстрела.
     * @param hit - попали ли в корабль
     * @param sunk - потопили ли корабль целиком
     */
    fun setShotResult(hit: Boolean, sunk: Boolean)
}