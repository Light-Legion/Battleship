package com.example.battleship_game.presentation.placement.manual

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.battleship_game.data.model.ShipPlacement

/**
 * ViewModel для экрана ручной расстановки кораблей.
 * Отвечает за генерацию и выдачу начального списка шаблонов кораблей.
 */
class ManualPlacementViewModel : ViewModel() {

    // Приватный MutableLiveData и публичный LiveData для шаблонов
    private val _templates = MutableLiveData(generateTemplates())
    val templates: LiveData<List<ShipPlacement>> = _templates

    companion object {
        /**
         * Собирает список шаблонов кораблей:
         * 1 шт по 4, 2 шт по 3, 3 шт по 2, 4 шт по 1.
         * Для шаблонов координаты startRow/startCol = 0, isVertical = false.
         */
        private fun generateTemplates(): List<ShipPlacement> {
            val list = mutableListOf<ShipPlacement>()

            // 1 четырёхпалубный
            list.add(ShipPlacement(
                shipId = 1,
                startRow = 0,
                startCol = 0,
                length = 4,
                isVertical = false
            ))

            // 2 трёхпалубных
            for (i in 0 until 2) {
                list.add(ShipPlacement(
                    shipId = 2 + i,
                    startRow = 0,
                    startCol = 0,
                    length = 3,
                    isVertical = false
                ))
            }

            // 3 двухпалубных
            for (i in 0 until 3) {
                list.add(ShipPlacement(
                    shipId = 4 + i,
                    startRow = 0,
                    startCol = 0,
                    length = 2,
                    isVertical = false
                ))
            }

            // 4 однопалубных
            for (i in 0 until 4) {
                list.add(ShipPlacement(
                    shipId = 7 + i,
                    startRow = 0,
                    startCol = 0,
                    length = 1,
                    isVertical = false
                ))
            }

            return list
        }
    }
}