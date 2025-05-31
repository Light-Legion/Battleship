package com.example.battleship_game.presentation.game

import androidx.lifecycle.ViewModel
import com.example.battleship_game.data.model.Difficulty
import com.example.battleship_game.data.model.ShipPlacement
import com.example.battleship_game.presentation.placement.PlacementStrategyType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

class LoadingActivityViewModel : ViewModel() {

    var playerShips: List<ShipPlacement> = emptyList()
    var computerShips: List<ShipPlacement> = emptyList()
    var difficulty: Difficulty = Difficulty.MEDIUM

    /**
     * Генерирует расстановку кораблей для компьютера
     * @param difficulty Уровень сложности для выбора стратегии
     * @return Список размещенных кораблей компьютера
     */
    suspend fun generateComputerShips(difficulty: Difficulty): List<ShipPlacement> {
        return withContext(Dispatchers.Default) {
            // Выбираем стратегию по уровню сложности
            val strategyType = PlacementStrategyType.fromDifficulty(difficulty)

            // Создаем экземпляр стратегии
            val strategy = strategyType.factory(Random.Default)

            // Генерируем и возвращаем расстановку
            strategy.generatePlacement()
        }
    }
}