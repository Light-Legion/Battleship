package com.example.battleship_game.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Описание расположения одного корабля.
 *
 * @property shipId      Уникальный идентификатор корабля (1–10).
 * @property startRow    Номер строки начала (0–9).
 * @property startCol    Номер столбца начала (0–9).
 * @property length      Количество палуб.
 * @property isVertical  true — вертикально, false — горизонтально.
 */
data class ShipPlacement(
    val shipId: Int,
    val startRow: Int,
    val startCol: Int,
    val length: Int,
    val isVertical: Boolean
)

/**
 * Сущность для сохранённых расстановок (`game_placement`).
 *
 * @property placementId    Авто-инкремент PK.
 * @property name           Название (≤20 символов).
 * @property placementJson  JSON-строка списка [ShipPlacement].
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