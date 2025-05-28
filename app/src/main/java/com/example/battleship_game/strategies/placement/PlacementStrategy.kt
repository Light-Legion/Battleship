package com.example.battleship_game.strategies.placement

import com.example.battleship_game.data.model.ShipPlacement

interface PlacementStrategy {
    fun generatePlacement(): List<ShipPlacement>
}