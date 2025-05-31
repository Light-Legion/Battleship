package com.example.battleship_game.data.model

import android.content.Context
import com.example.battleship_game.R
import com.example.battleship_game.data.model.Difficulty.EASY
import com.example.battleship_game.data.model.Difficulty.HARD
import com.example.battleship_game.data.model.Difficulty.MEDIUM

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

    companion object {
        /**
         * Конвертирует строку (например, "Легкий", "Средний", "Сложный")
         * в соответствующий enum-элемент [Difficulty].
         * Если ни один из вариантов не подходит, возвращает MEDIUM по умолчанию.
         */
        fun fromString(context: Context, str: String): GameResult {
            return when (str) {
                context.getString(R.string.result_win) -> WIN
                context.getString(R.string.result_loss) -> LOSS
                else -> LOSS
            }
        }
    }
}