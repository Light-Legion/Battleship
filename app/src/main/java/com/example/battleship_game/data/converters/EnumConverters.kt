package com.example.battleship_game.data.converters

import androidx.room.TypeConverter
import com.example.battleship_game.data.model.Difficulty
import com.example.battleship_game.data.model.GameResult

/**
 * Конвертеры enum ↔ String для Room.
 */
class EnumConverters {
    @TypeConverter
    fun fromDifficulty(level: Difficulty): String = level.name

    @TypeConverter
    fun toDifficulty(name: String): Difficulty = Difficulty.valueOf(name)

    @TypeConverter
    fun fromGameResult(r: GameResult): String = r.name

    @TypeConverter
    fun toGameResult(name: String): GameResult = GameResult.valueOf(name)
}