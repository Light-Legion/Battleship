package com.example.battleship_game.presentation.placement

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.battleship_game.data.db.AppDatabase
import com.example.battleship_game.data.entity.GameField
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * ViewModel экрана «Загрузка сохранённой расстановки».
 * Держит Flow<List<GameField>> для UI.
 */
class LoadSavedFieldViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.getInstance(app).gameFieldDao()
    private val _fields = MutableStateFlow<List<GameField>>(emptyList())
    val fields: StateFlow<List<GameField>> = _fields

    init {
        dao.getAll()
            .onEach { _fields.value = it }
            .launchIn(viewModelScope)
    }
}