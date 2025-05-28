package com.example.battleship_game.strategies.shooting

interface ShootingStrategy {
    fun getNextShot() : Pair<Int, Int>
    fun setShotResult(hit: Boolean, sunk: Boolean)
}