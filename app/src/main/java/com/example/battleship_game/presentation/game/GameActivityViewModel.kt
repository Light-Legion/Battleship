package com.example.battleship_game.presentation.game

import androidx.lifecycle.ViewModel
import com.example.battleship_game.data.model.ShipPlacement

class GameActivityViewModel : ViewModel() {

    lateinit var playerShips: List<ShipPlacement>
    lateinit var computerShips: List<ShipPlacement>

}