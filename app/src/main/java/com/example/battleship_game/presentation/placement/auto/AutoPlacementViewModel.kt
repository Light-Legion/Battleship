package com.example.battleship_game.presentation.placement.auto

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.battleship_game.R
import com.example.battleship_game.data.model.ShipPlacement
import com.example.battleship_game.strategies.placement.CoastsPlacer
import com.example.battleship_game.strategies.placement.DiagonalPlacer
import com.example.battleship_game.strategies.placement.HalfFieldPlacer
import kotlin.random.Random

/**
 * Хранит текущую расстановку и сигнализирует об её наличии,
 * чтобы включать/выключать кнопки «Сохранить»/«В бой».
 */
class AutoPlacementViewModel : ViewModel() {

    private val _has = MutableLiveData(false)
    val hasPlacementLive: LiveData<Boolean> = _has

    private var _current = emptyList<ShipPlacement>()
    val currentPlacement: List<ShipPlacement> get() = _current

    fun generatePlacement(ctx: Context, name: String): List<ShipPlacement> {
        val rand = Random.Default
        val strategy = when (name) {
            ctx.getString(R.string.strategy_placement_coasts)       -> CoastsPlacer(rand)
            ctx.getString(R.string.strategy_placement_half_field)   -> HalfFieldPlacer(rand)
            ctx.getString(R.string.strategy_placement_diagonal)     -> DiagonalPlacer(rand)
            else -> CoastsPlacer(rand)
        }
        val res = strategy.generatePlacement()
        _current = res
        _has.value = res.isNotEmpty()
        return res
    }
}