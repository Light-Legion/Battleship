package com.example.battleship_game.presentation.game

import com.example.battleship_game.data.model.Difficulty
import com.example.battleship_game.strategies.shooting.DensityAnalysisShooter
import com.example.battleship_game.strategies.shooting.DiagonalShooter
import com.example.battleship_game.strategies.shooting.RandomFinishingShooter
import com.example.battleship_game.strategies.shooting.ShootingStrategy

/**
 * Фáбрика для получения конкретной стратегии стрельбы
 * по переданному уровню сложности.
 */
object ShootingStrategyFactory {
    fun createForDifficulty(difficulty: Difficulty): ShootingStrategy {
        return when (difficulty) {
            Difficulty.EASY   -> RandomFinishingShooter()
            Difficulty.MEDIUM -> DiagonalShooter()
            Difficulty.HARD   -> DensityAnalysisShooter()
        }
    }
}