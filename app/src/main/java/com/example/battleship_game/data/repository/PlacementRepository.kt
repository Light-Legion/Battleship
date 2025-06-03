package com.example.battleship_game.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.example.battleship_game.data.dao.GamePlacementDao
import com.example.battleship_game.data.entity.GamePlacement
import com.example.battleship_game.data.model.ShipPlacement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Репозиторий для работы с сохранёнными расстановками.
 *
 * Инкапсулирует вызовы GamePlacementDao и
 * преобразует GamePlacement.placement (List<ShipPlacement>)
 * в LiveData<List<ShipPlacement>>.
 */
class PlacementRepository(
    private val dao: GamePlacementDao
) {
    /**
     * Возвращает Flow со списком всех сохранённых Entity (GamePlacement).
     * Если нужно, можно обернуть в map { it.map { entity -> entity.placement } }.
     */
    fun getAllEntities(): Flow<List<GamePlacement>> = dao.getAll()

    /**
     * Возвращает LiveData<List<ShipPlacement>> по заданному ID.
     * Если записи нет, возвращает пустой список.
     */
    fun getPlacementById(id: Long): LiveData<List<ShipPlacement>> {
        return dao.getByIdFlow(id)
            .map { entity ->
                entity?.placement ?: emptyList()
            }
            .asLiveData()
    }

    /**
     * Сохранить новую Entity. Room сам сериализует List<ShipPlacement> в JSON.
     */
    suspend fun insertPlacement(entity: GamePlacement) {
        dao.insert(entity)
    }
}