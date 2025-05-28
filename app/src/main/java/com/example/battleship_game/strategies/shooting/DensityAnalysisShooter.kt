package com.example.battleship_game.strategies.shooting

class DensityAnalysisShooter : ShootingStrategy {
    private val tried = Array(10){ BooleanArray(10){false} }
    private val remainingShips = mutableListOf(4,3,3,2,2,2,1,1,1,1)
    private var lastShot: Pair<Int,Int>? = null

    override fun getNextShot(): Pair<Int, Int> {
        val probability = Array(10){ IntArray(10){0} }
        // Для каждого оставшегося корабля
        for (size in remainingShips) {
            // Горизонтальные размещения
            for (y in 0..9) for (x in 0..10-size) {
                if ((0 until size).all { !tried[y][x+it] }) {
                    for (i in 0 until size) probability[y][x+i]++
                }
            }
            // Вертикальные размещения
            for (x in 0..9) for (y in 0..10-size) {
                if ((0 until size).all { !tried[y+it][x] }) {
                    for (i in 0 until size) probability[y+i][x]++
                }
            }
        }
        // Выбираем клетку с максимальным значением
        var best = Pair(0,0); var maxProb = -1
        for (y in 0..9) for (x in 0..9) {
            if (!tried[y][x] && probability[y][x] > maxProb) {
                maxProb = probability[y][x]; best = Pair(x,y)
            }
        }
        lastShot = best
        return best
    }

    override fun setShotResult(hit: Boolean, sunk: Boolean) {
        val (x,y) = lastShot!!
        tried[y][x] = true
        if (sunk) {
            // При потоплении можно убрать из remainingShips размер потопленного (реализация зависит от механики)
        }
    }
}