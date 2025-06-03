package com.example.battleship_game.presentation.placement.save

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.battleship_game.data.db.AppDatabase
import com.example.battleship_game.data.entity.GamePlacement
import com.example.battleship_game.data.model.ShipPlacement
import com.example.battleship_game.data.repository.PlacementRepository
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ViewModel экрана «Сохранить расстановку».
 * Отвечает за сериализацию и сохранение в БД.
 */
class SavePlacementViewModel(app: Application) : AndroidViewModel(app) {

    // Получаем DAO, создаём репозиторий
    private val repository: PlacementRepository

    private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    init {
        val dao = AppDatabase.getInstance(app).gamePlacementDao()
        repository = PlacementRepository(dao)
    }

    /**
     * Сохранить расстановку.
     *
     * @param name  Введённое пользователем название (уже валидированное).
     * @param ships Список [com.example.battleship_game.data.model.ShipPlacement], полученный из Intent.
     */
    fun save(name: String, ships: List<ShipPlacement>) {
        viewModelScope.launch {
            val now = dateFormat.format(Date())
            val entity = GamePlacement(
                name = name,
                placement = ships,
                date = now
            )
            // Вместо dao.insert(...) вызываем репозиторий
            repository.insertPlacement(entity)
        }
    }
}