package com.example.battleship_game.strategies.placement

/** Проверяет выход или занятость */
internal fun Array<BooleanArray>.isOccupied(r: Int, c: Int): Boolean =
    r !in indices || c !in this[0].indices || this[r][c]

/** Помечает занятость, если в пределах */
internal fun Array<BooleanArray>.markOccupied(r: Int, c: Int) {
    if (r in indices && c in this[0].indices) this[r][c] = true
}

/** Можно ли разместить корабль + буфер вокруг него? */
internal fun canPlace(
    occupied: Array<BooleanArray>,
    x0: Int, y0: Int,
    size: Int,
    horiz: Boolean
): Boolean {
    val dx = if (horiz) 1 else 0
    val dy = if (horiz) 0 else 1
    for (k in 0 until size) {
        val x = x0 + dx*k
        val y = y0 + dy*k
        for (ry in y-1..y+1) for (rx in x-1..x+1) {
            if (occupied.isOccupied(ry, rx)) return false
        }
    }
    return true
}

/** Помечаем корабль + буферку */
internal fun markOccupiedWithBuffer(
    occupied: Array<BooleanArray>,
    x0: Int, y0: Int,
    size: Int,
    horiz: Boolean
) {
    val dx = if (horiz) 1 else 0
    val dy = if (horiz) 0 else 1
    for (k in 0 until size) {
        val x = x0 + dx*k
        val y = y0 + dy*k
        for (ry in y-1..y+1) for (rx in x-1..x+1) {
            occupied.markOccupied(ry, rx)
        }
    }
}
