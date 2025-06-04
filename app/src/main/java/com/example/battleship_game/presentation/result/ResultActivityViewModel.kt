package com.example.battleship_game.presentation.result

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.battleship_game.data.db.AppDatabase
import com.example.battleship_game.data.entity.GameHistory
import com.example.battleship_game.data.model.Difficulty
import com.example.battleship_game.data.model.GameResult
import com.example.battleship_game.data.model.ShipPlacement
import com.example.battleship_game.data.repository.GameHistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ResultActivityViewModel(app: Application) : AndroidViewModel(app) {

    private val repository: GameHistoryRepository

    var ships: List<ShipPlacement> = emptyList()
    var result: GameResult = GameResult.LOSS
    var level: Difficulty = Difficulty.MEDIUM

    init {
        val dao = AppDatabase.getInstance(app).gameHistoryDao()
        repository = GameHistoryRepository(dao)
    }

    /**
     * Сохраняет запись об игре в историю.
     */
    fun saveGameHistory(name: String, result: GameResult, level: Difficulty) {
        val now = LocalDateTime.now()
        val date = now.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
        val history = GameHistory(
            name = name,
            result = result,
            level = level,
            date = date
        )
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertHistory(history)
        }
    }
}