package com.example.battleship_game.data.repository

import com.example.battleship_game.data.dao.GameHistoryDao
import com.example.battleship_game.data.entity.GameHistory
import kotlinx.coroutines.flow.Flow


/**
 * Репозиторий, инкапсулирующий работу с историей игр.
 */
class GameHistoryRepository(
    private val gameHistoryDao: GameHistoryDao
) {

    /**
     * Возвращает Flow всех записей (от новых к старым).
     */
    fun getAllGameHistory(): Flow<List<GameHistory>> {
        return gameHistoryDao.getAll()
    }

    /**
     * Вставляет новую запись об окончании игры.
     */
    suspend fun insertHistory(history: GameHistory) {
        gameHistoryDao.insert(history)
    }
}