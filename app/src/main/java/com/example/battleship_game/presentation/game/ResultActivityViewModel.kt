package com.example.battleship_game.presentation.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.battleship_game.data.db.AppDatabase
import com.example.battleship_game.data.entity.GameHistory
import com.example.battleship_game.data.model.Difficulty
import com.example.battleship_game.data.model.GameResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ResultActivityViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDatabase.getInstance(app).gameHistoryDao()

    // extension: можно использовать viewModelScope
    fun saveGameHistory(
        name: String,
        result: GameResult,
        level: Difficulty,
        date: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val gameHistory = GameHistory(
                name = name,
                result = result,
                level = level,
                date = date
            )
            dao.insert(gameHistory)
        }
    }
}