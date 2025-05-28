package com.example.battleship_game.strategies.placement

import com.example.battleship_game.data.model.ShipPlacement

/**
 * Базовый интерфейс для стратегий автоматической расстановки кораблей.
 */
interface PlacementStrategy {
    /**
     * Возвращает список из 10 объектов ShipPlacement,
     * удовлетворяющих своей стратегии (размер, координаты, ориентация).
     */
    fun generatePlacement(): List<ShipPlacement>
}