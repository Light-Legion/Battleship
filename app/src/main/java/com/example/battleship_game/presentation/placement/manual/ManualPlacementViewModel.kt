package com.example.battleship_game.presentation.placement.manual

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.battleship_game.data.db.AppDatabase
import com.example.battleship_game.data.model.Difficulty
import com.example.battleship_game.data.model.ShipPlacement
import com.example.battleship_game.data.repository.PlacementRepository
import kotlinx.coroutines.launch

/**
 * ViewModel для экрана ручной расстановки кораблей.
 * Отвечает за генерацию и выдачу начального списка шаблонов кораблей.
 */
class ManualPlacementViewModel(application: Application) : AndroidViewModel(application) {

    // Приватный MutableLiveData и публичный LiveData для шаблонов
    private val _templates = MutableLiveData(generateTemplates())
    val templates: LiveData<List<ShipPlacement>> = _templates
    var fieldID : Long = -1L
    var difficulty: Difficulty = Difficulty.MEDIUM

    /**
     * Репозиторий для работы с сохранёнными расстановками.
     * Мы получаем DAO через AppDatabase.getInstance(application), а затем передаем DAO в репозиторий.
     */
    private val repository: PlacementRepository by lazy {
        val dao = AppDatabase.getInstance(application).gamePlacementDao()
        PlacementRepository(dao)
    }

    private val _savedPlacements = MutableLiveData<List<ShipPlacement>>(emptyList())
    val savedPlacements: LiveData<List<ShipPlacement>> = _savedPlacements

    /**
     * Запускает одноразовое чтение списка ShipPlacement из базы.
     * Если fieldID == -1, просто оставляем пустой список.
     * Иначе — вызываем suspend-функцию repository.getPlacementById(id).
     *
     * Данный метод должен быть вызван после того, как fieldID был установлен
     * (обычно — сразу после чтения из Intent-а в Activity).
     */
    fun loadSavedPlacement() {
        if (fieldID < 0) {
            // Нет сохранённой расстановки, оставляем пустой список
            _savedPlacements.postValue(emptyList())
            return
        }

        viewModelScope.launch {
            try {
                // Suspend-функция getPlacementById(id) вернёт List<ShipPlacement>
                val listFromDb: List<ShipPlacement> = repository.getPlacementById(fieldID)
                _savedPlacements.postValue(listFromDb)
            } catch (e: Exception) {
                // В случае ошибки (например, БД недоступна) оставляем пустой список
                _savedPlacements.postValue(emptyList())
            }
        }
    }

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