package com.example.battleship_game.presentation.placement

import android.content.Context
import com.example.battleship_game.R
import com.example.battleship_game.data.model.Difficulty
import com.example.battleship_game.strategies.placement.CoastsPlacer
import com.example.battleship_game.strategies.placement.DiagonalPlacer
import com.example.battleship_game.strategies.placement.HalfFieldPlacer
import com.example.battleship_game.strategies.placement.PlacementStrategy
import kotlin.random.Random

sealed class PlacementStrategyType(
    val factory: (rand: Random) -> PlacementStrategy
) {
    object Coasts    : PlacementStrategyType(::CoastsPlacer)
    object HalfField : PlacementStrategyType(::HalfFieldPlacer)
    object Diagonal  : PlacementStrategyType(::DiagonalPlacer)

    companion object {
        /**
         * Парсит строковое значение из ресурсов (из выпадающего списка) в соответствующий тип.
         */
        fun fromString(ctx: Context, str: String): PlacementStrategyType = when (str) {
            ctx.getString(R.string.strategy_placement_coasts)     -> Coasts
            ctx.getString(R.string.strategy_placement_half_field) -> HalfField
            ctx.getString(R.string.strategy_placement_diagonal)   -> Diagonal
            else -> Coasts
        }

        /**
         * По [Difficulty] возвращает нужный тип стратегии.
         */
        fun fromDifficulty(diff: Difficulty): PlacementStrategyType = when (diff) {
            Difficulty.EASY   -> HalfField
            Difficulty.MEDIUM -> Coasts
            Difficulty.HARD   -> Diagonal
        }
    }
}
