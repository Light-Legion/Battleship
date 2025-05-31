package com.example.battleship_game.strategies.placement

import kotlin.random.Random

/**
 * Расставляет корабли на границах поля (строки 0 или 9 для горизонтали,
 * столбцы 0 или 9 для вертикали).
 */
class CoastsPlacer(rand: Random = Random.Default)
    : BasePlacementStrategy(rand) {

    override fun scanCells(): List<Pair<Int, Int>> {
        val out = mutableListOf<Pair<Int, Int>>()
        // верхняя и нижняя строка: (r=0, c=0..9), (r=9, c=0..9)
        for (c in 0..9) {
            out += 0 to c
            out += 9 to c
        }
        // левая и правая колонка: (r=1..8, c=0), (r=1..8, c=9)
        for (r in 1..8) {
            out += r to 0
            out += r to 9
        }
        return out
    }

    // Переопределяем canPlace, чтобы запретить палубы на диагоналях
    override fun canPlace(
        occ: Array<BooleanArray>,
        x0: Int, y0: Int,
        size: Int,
        horizontal: Boolean
    ): Boolean {
        val requiredOrientation = when {
            // Верхняя/нижняя граница - только горизонтальная ориентация
            y0 == 0 || y0 == 9 -> true
            // Левая/правая граница - только вертикальная ориентация
            x0 == 0 || x0 == 9 -> false
            // Для клеток рядом с границей разрешаем обе ориентации
            else -> horizontal
        }

        // Если запрошенная ориентация не соответствует требуемой - отказ
        if (horizontal != requiredOrientation) return false

        return super.canPlace(occ, x0, y0, size, horizontal)
    }
}