package com.example.battleship_game.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Сущность для сохранённых расстановок (`game_placement`).
 *
 * @property placementId    Авто-инкремент PK.
 * @property name           Название (≤20 символов).
 * @property placementJson  JSON-строка списка [com.example.battleship_game.data.model.ShipPlacement].
 * @property date           Время создания в формате "dd.MM.yyyy HH:mm".
 */
@Entity(tableName = "game_placement")
data class GamePlacement(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "placement_id")
    val placementId: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "placement_json")
    val placementJson: String,

    @ColumnInfo(name = "date")
    val date: String
)