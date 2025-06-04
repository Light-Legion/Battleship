package com.example.battleship_game.presentation.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.battleship_game.data.db.AppDatabase
import com.example.battleship_game.data.entity.GameHistory
import com.example.battleship_game.data.repository.GameHistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * ViewModel экрана статистики.
 * Загружает данные из DAO и публикует в StateFlow для UI.
 */
class StatsViewModel(app: Application) : AndroidViewModel(app) {
    private val repository: GameHistoryRepository
    private val _stats = MutableStateFlow<List<GameHistory>>(emptyList())
    val stats: StateFlow<List<GameHistory>> = _stats

    init {
        val dao = AppDatabase.getInstance(app).gameHistoryDao()
        repository = GameHistoryRepository(dao)
        // Подписываемся на поток записей из репозитория
        viewModelScope.launch {
            repository.getAllGameHistory()
                .onEach { list -> _stats.value = list }
                .launchIn(viewModelScope)
        }
    }
}