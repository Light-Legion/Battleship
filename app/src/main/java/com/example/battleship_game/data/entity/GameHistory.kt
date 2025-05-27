package com.example.battleship_game.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

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


/**
 * Сущность для таблицы `game_progress`.
 *
 * @property gameId    Автоинкрементный первичный ключ.
 * @property name      Имя игрока (до 20 символов).
 * @property result    Результат игры (WIN/LOSS).
 * @property level     Уровень сложности (EASY/MEDIUM/HARD).
 * @property date      Дата-время в формате "дд.MM.yyyy HH:mm".
 */
@Entity(tableName = "game_progress")
data class GameHistory(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "game_id")
    val gameId: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "result")
    val result: GameResult,

    @ColumnInfo(name = "level")
    val level: Difficulty,

    @ColumnInfo(name = "date")
    val date: String
)
