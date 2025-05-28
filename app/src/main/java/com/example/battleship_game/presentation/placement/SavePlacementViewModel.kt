package com.example.battleship_game.presentation.placement


import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.battleship_game.data.db.AppDatabase
import com.example.battleship_game.data.entity.GamePlacement
import com.example.battleship_game.data.model.ShipPlacement
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * ViewModel экрана «Сохранить расстановку».
 * Отвечает за сериализацию и сохранение в БД.
 */
class SavePlacementViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDatabase.getInstance(app).gamePlacementDao()
    private val gson = Gson()
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    /**
     * Сохранить расстановку.
     *
     * @param name  Введённое пользователем название (уже валидированное).
     * @param ships Список [ShipPlacement], полученный из Intent.
     */
    fun save(name: String, ships: List<ShipPlacement>) {
        viewModelScope.launch {
            val now = dateFormat.format(Date())
            // Превращаем список кораблей в JSON
            val json = gson.toJson(ships)
            val entity = GamePlacement(
                name           = name,
                placementJson  = json,
                date           = now
            )
            dao.insert(entity)
        }
    }
}