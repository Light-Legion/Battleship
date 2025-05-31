package com.example.battleship_game.data.model

import android.content.Context
import com.example.battleship_game.R

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

    companion object {
        /**
         * Конвертирует строку (например, "Легкий", "Средний", "Сложный")
         * в соответствующий enum-элемент [Difficulty].
         * Если ни один из вариантов не подходит, возвращает MEDIUM по умолчанию.
         */
        fun fromString(context: Context, str: String): Difficulty {
            return when (str) {
                context.getString(R.string.difficulty_easy) -> EASY
                context.getString(R.string.difficulty_medium) -> MEDIUM
                context.getString(R.string.difficulty_hard) -> HARD
                else -> MEDIUM
            }
        }
    }
}