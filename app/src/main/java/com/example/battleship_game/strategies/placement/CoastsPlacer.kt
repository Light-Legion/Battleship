package com.example.battleship_game.strategies.placement

import kotlin.random.Random

class CoastsPlacer(rand: Random = Random.Default)
    : BasePlacementStrategy(rand) {
    override fun scanCells(): List<Pair<Int, Int>> {
        val out = mutableListOf<Pair<Int, Int>>()
        // верхняя и нижняя строки
        for (c in 0..9) {
            out += 0 to c
            out += 9 to c
        }
        // левая и правая колонка (без углов, чтобы не дублировать)
        for (r in 1..8) {
            out += r to 0
            out += r to 9
        }
        return out
    }
}