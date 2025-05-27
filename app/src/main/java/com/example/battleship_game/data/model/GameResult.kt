package com.example.battleship_game.data.model

/**
 * Результат игры: ПОБЕДА или ПОРАЖЕНИЕ.
 */
enum class GameResult {
    WIN, LOSS;

    /** Отображаемое русское название */
    fun toDisplayString(): String =
        when(this) {
            WIN  -> "Победа"
            LOSS -> "Поражение"
        }
}