package com.example.battleship_game.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.battleship_game.data.entity.GameField
import kotlinx.coroutines.flow.Flow

/**
 * DAO для таблицы `game_field`.
 */
@Dao
interface GameFieldDao {
    /**
     * Сохранить новую расстановку.
     * При конфликте по PK — игнорируем.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(field: GameField)

    /**
     * Возвращает все сохранённые расстановки, отсортированные по убыванию ID.
     */
    @Query("SELECT * FROM game_field ORDER BY field_id DESC")
    fun getAll(): Flow<List<GameField>>

    /**
     * Получить конкретную расстановку по ID.
     */
    @Query("SELECT * FROM game_field WHERE field_id = :id LIMIT 1")
    suspend fun getById(id: Long): GameField?

}