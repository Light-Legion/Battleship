package com.example.battleship_game.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.example.battleship_game.data.dao.GamePlacementDao
import com.example.battleship_game.data.entity.GamePlacement
import com.example.battleship_game.data.model.ShipPlacement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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
     * Возвращает <List<ShipPlacement>> по заданному ID.
     * Если записи нет, возвращает пустой список.
     */
    suspend fun getPlacementById(id: Long): List<ShipPlacement> {
        // Здесь мы берём Flow<GamePlacement?>, превращаем его в Flow<List<ShipPlacement>>,
        // а затем читаем первое значение через first().
        val placementList: List<ShipPlacement> = dao
            .getByIdFlow(id)              // Flow<GamePlacement?>
            .map { entity ->
                // Если entity не null, возвращаем entity.placement (List<ShipPlacement>), иначе пустой список
                entity?.placement ?: emptyList()
            }
            .first()                      // берём первое (и единственное) эмитируемое значение
        return placementList
    }

    /**
     * Сохранить новую Entity. Room сам сериализует List<ShipPlacement> в JSON.
     */
    suspend fun insertPlacement(entity: GamePlacement) {
        dao.insert(entity)
    }
}