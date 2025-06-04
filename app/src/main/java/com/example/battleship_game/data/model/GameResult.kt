package com.example.battleship_game.data.model

import android.content.Context
import com.example.battleship_game.R

/**
 * Результат игры: ПОБЕДА или ПОРАЖЕНИЕ.
 */
enum class GameResult(val displayNameRes: Int) {
    WIN(R.string.result_win),
    LOSS(R.string.result_loss);

    /** Возвращает локализованную строку по ресурсу */
    fun toDisplayString(context: Context): String {
        return context.getString(displayNameRes)
    }

    companion object {
        /** Возвращает enum по локализованной строке */
        fun fromDisplayName(context: Context, str: String): GameResult {
            return entries.firstOrNull { context.getString(it.displayNameRes) == str } ?: LOSS
        }

        /** Возвращает список всех локализованных названий */
        fun getDisplayNames(context: Context): List<String> {
            return entries.map { context.getString(it.displayNameRes) }
        }
    }
}