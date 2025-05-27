package com.example.battleship_game.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.battleship_game.data.entity.GamePlacement
import kotlinx.coroutines.flow.Flow

/**
 * DAO для `game_placement`.
 */
@Dao
interface GamePlacementDao {

    /** Вставить новую; при конфликте по PK — игнорируем. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(placement: GamePlacement)

    /** Все расстановки, от новых к старым. */
    @Query("SELECT * FROM game_placement ORDER BY placement_id DESC")
    fun getAll(): Flow<List<GamePlacement>>

    /** Одна расстановка по ID */
    @Query("SELECT * FROM game_placement WHERE placement_id = :id LIMIT 1")
    suspend fun getById(id: Long): GamePlacement?
}