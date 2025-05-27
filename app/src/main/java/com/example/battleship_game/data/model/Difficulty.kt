package com.example.battleship_game.data.model

/**
 * Уровень сложности: ЛЕГКИЙ, СРЕДНИЙ, СЛОЖНЫЙ.
 */
enum class Difficulty {
    EASY, MEDIUM, HARD;

    /** Отображаемое русское название */
    fun toDisplayString(): String =
        when(this) {
            EASY   -> "Легкий"
            MEDIUM -> "Средний"
            HARD   -> "Сложный"
        }
}