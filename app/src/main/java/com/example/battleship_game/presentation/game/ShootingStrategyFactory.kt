package com.example.battleship_game.presentation.game

import com.example.battleship_game.data.model.Difficulty
import com.example.battleship_game.strategies.shooting.DensityAnalysisStrategy
import com.example.battleship_game.strategies.shooting.CombinedDiagonalStrategy
import com.example.battleship_game.strategies.shooting.RandomFinishingStrategy
import com.example.battleship_game.strategies.shooting.ShootingStrategy

/**
 * Фáбрика для получения конкретной стратегии стрельбы
 * по переданному уровню сложности.
 */
object ShootingStrategyFactory {
    fun createForDifficulty(difficulty: Difficulty): ShootingStrategy {
        return when (difficulty) {
            Difficulty.EASY   -> RandomFinishingStrategy()
            Difficulty.MEDIUM -> CombinedDiagonalStrategy()
            Difficulty.HARD   -> DensityAnalysisStrategy()
        }
    }
}