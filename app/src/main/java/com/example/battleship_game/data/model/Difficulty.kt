package com.example.battleship_game.data.model

import android.content.Context
import com.example.battleship_game.R

/**
 * Уровень сложности: ЛЕГКИЙ, СРЕДНИЙ, СЛОЖНЫЙ.
 */
enum class Difficulty(val displayNameRes: Int) {
    EASY(R.string.difficulty_easy),
    MEDIUM(R.string.difficulty_medium),
    HARD(R.string.difficulty_hard);

    /** Возвращает локализованную строку по ресурсу */
    fun toDisplayString(context: Context): String {
        return context.getString(displayNameRes)
    }

    companion object {
        /** Возвращает enum по локализованной строке */
        fun fromDisplayName(context: Context, str: String): Difficulty {
            return entries.firstOrNull { context.getString(it.displayNameRes) == str } ?: MEDIUM
        }

        /** Возвращает список всех локализованных названий */
        fun getDisplayNames(context: Context): List<String> {
            return entries.map { context.getString(it.displayNameRes) }
        }
    }
}