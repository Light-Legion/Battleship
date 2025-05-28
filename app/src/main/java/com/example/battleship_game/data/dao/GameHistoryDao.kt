package com.example.battleship_game.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.battleship_game.data.entity.GameHistory
import kotlinx.coroutines.flow.Flow

/**
 * DAO для таблицы `game_progress`.
 */
@Dao
interface GameHistoryDao {
    /**
     * Вставить новую запись. Если дубликат по PK — игнорируем.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(progress: GameHistory)

    /**
     * Вернуть все записи, сортируя по убыванию ID (последние сверху).
     */
    @Query("SELECT * FROM game_history ORDER BY game_id DESC")
    fun getAll(): Flow<List<GameHistory>>
}