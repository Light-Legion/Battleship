package com.example.battleship_game.presentation.placement

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.battleship_game.data.db.AppDatabase
import com.example.battleship_game.data.entity.GamePlacement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * ViewModel для LoadSavedPlacementActivity.
 */
class LoadSavedPlacementViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.getInstance(app).gamePlacementDao()
    private val _placements = MutableStateFlow<List<GamePlacement>>(emptyList())
    val placements: StateFlow<List<GamePlacement>> = _placements

    init {
        dao.getAll()
            .onEach { _placements.value = it }
            .launchIn(viewModelScope)
    }
}