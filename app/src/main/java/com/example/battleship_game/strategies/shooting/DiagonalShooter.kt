package com.example.battleship_game.strategies.shooting

class DiagonalShooter : ShootingStrategy {
    private val rand = java.util.Random()
    private val tried = Array(10){ BooleanArray(10){false} }
    private val pattern = mutableListOf<Pair<Int,Int>>()
    private var lastShot: Pair<Int,Int>? = null

    init {
        // Заполняем список клеток вида шахматной доски
        for (y in 0..9) {
            for (x in 0..9 step 2) {
                pattern.add(Pair((x + (y % 2)), y))
            }
        }
        pattern.shuffle()
    }

    override fun getNextShot(): Pair<Int, Int> {
        // Выбираем первую доступную в сгенерированном паттерне
        while (pattern.isNotEmpty()) {
            val (x,y) = pattern.removeAt(0)
            if (!tried[y][x]) {
                lastShot = Pair(x,y)
                return Pair(x,y)
            }
        }
        // Если паттерн исчерпан, просто случайно
        var x: Int; var y: Int
        do {
            x = rand.nextInt(10); y = rand.nextInt(10)
        } while (tried[y][x])
        lastShot = Pair(x,y)
        return Pair(x,y)
    }

    override fun setShotResult(hit: Boolean, sunk: Boolean) {
        val (x,y) = lastShot!!
        tried[y][x] = true
    }
}