package com.example.battleship_game.strategies.shooting

class RandomFinishingShooter : ShootingStrategy {
    private val rand = java.util.Random()
    private val tried = Array(10){ BooleanArray(10){false} }
    private val targetQueue = ArrayDeque<Pair<Int,Int>>()
    private var lastShot: Pair<Int,Int>? = null

    override fun getNextShot(): Pair<Int, Int> {
        // Если есть цели из очереди добивания, берём первую
        while (targetQueue.isNotEmpty()) {
            val (x,y) = targetQueue.removeFirst()
            if (x in 0..9 && y in 0..9 && !tried[y][x]) {
                lastShot = Pair(x,y)
                return Pair(x,y)
            }
        }
        // Иначе случайная неизбитая клетка
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
        if (hit && !sunk) {
            // Добавляем соседние клетки для добивания
            targetQueue.add(Pair(x+1,y))
            targetQueue.add(Pair(x-1,y))
            targetQueue.add(Pair(x,y+1))
            targetQueue.add(Pair(x,y-1))
        }
        if (hit && sunk) {
            // При потоплении сбрасываем очередь добивания
            targetQueue.clear()
        }
    }
}