package com.example.battleship_game.presentation.placement

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.battleship_game.R
import com.example.battleship_game.data.model.ShipPlacement
import com.example.battleship_game.strategies.placement.CoastsPlacer
import com.example.battleship_game.strategies.placement.DiagonalPlacer
import com.example.battleship_game.strategies.placement.HalfFieldPlacer
import com.example.battleship_game.strategies.placement.PlacementStrategy

/**
 * Хранит текущую расстановку и сигнализирует об её наличии,
 * чтобы включать/выключать кнопки «Сохранить»/«В бой».
 */
class AutoPlacementViewModel : ViewModel() {

    /** Текущая расстановка кораблей (или null). */
    var currentPlacement: List<ShipPlacement>? = null
        set(value) {
            field = value
            _hasPlacement.value = !value.isNullOrEmpty()
        }

    private val _hasPlacement = MutableLiveData(false)
    /** Доступность кнопок «Сохранить»/«В бой». */
    val hasPlacementLive: LiveData<Boolean> = _hasPlacement

    /** Удобный локальный флаг. */
    var hasPlacement: Boolean
        get() = _hasPlacement.value == true
        set(v) { _hasPlacement.value = v }

    /**
     * Фабрика: по локализованному имени возвращает стратегию.
     * @return экземпляр [PlacementStrategy] или null, если имя не распознано.
     */
    fun getStrategyForName(ctx: Context, name: String): PlacementStrategy? = when (name) {
        ctx.getString(R.string.strategy_placement_half_field)     -> HalfFieldPlacer()
        ctx.getString(R.string.strategy_placement_coasts)         -> CoastsPlacer()
        ctx.getString(R.string.strategy_placement_diagonal)       -> DiagonalPlacer()
        else                                                     -> null
    }
}