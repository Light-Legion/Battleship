package com.example.battleship_game.presentation.placement.auto

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.example.battleship_game.data.model.Difficulty
import com.example.battleship_game.data.model.ShipPlacement
import com.example.battleship_game.presentation.placement.PlacementStrategyType
import kotlin.random.Random

/**
 * Хранит текущую расстановку и сигнализирует об её наличии,
 * чтобы включать/выключать кнопку «В бой».
 */
class AutoPlacementViewModel : ViewModel() {

    private val _placement = MutableLiveData<List<ShipPlacement>>(emptyList())
    val placement: LiveData<List<ShipPlacement>> = _placement

    // Флаг «наличия» расстановки
    val hasPlacementLive: LiveData<Boolean> = _placement.map { it.isNotEmpty() }

    var difficulty: Difficulty = Difficulty.MEDIUM

    fun generate(type: PlacementStrategyType) {
        // опционально: запуск в background
        val rand = Random.Default
        val strategy = type.factory(rand)
        _placement.value = strategy.generatePlacement()
    }
}