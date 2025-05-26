package com.example.battleship_game.presentation.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.battleship_game.data.db.AppDatabase
import com.example.battleship_game.data.entity.GameProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * ViewModel экрана статистики.
 * Загружает данные из DAO и публикует в StateFlow для UI.
 */
class StatsViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.getInstance(app).gameProgressDao()
    private val _stats = MutableStateFlow<List<GameProgress>>(emptyList())
    val stats: StateFlow<List<GameProgress>> = _stats

    init {
        // Подписываемся на поток всех записей из БД
        dao.getAll()
            .onEach { _stats.value = it }
            .launchIn(viewModelScope)
    }
}