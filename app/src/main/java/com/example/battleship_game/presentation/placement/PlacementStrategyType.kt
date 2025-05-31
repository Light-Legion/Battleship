package com.example.battleship_game.presentation.placement

import android.content.Context
import com.example.battleship_game.R
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
        fun fromString(ctx: Context, str: String): PlacementStrategyType = when (str) {
            ctx.getString(R.string.strategy_placement_coasts)     -> Coasts
            ctx.getString(R.string.strategy_placement_half_field) -> HalfField
            ctx.getString(R.string.strategy_placement_diagonal)   -> Diagonal
            else -> Coasts
        }
    }
}
