package com.example.battleship_game.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Сущность для сохранённых расстановок кораблей.
 *
 * @property fieldId   Автоинкрементный PK.
 * @property name      Название расстановки (<=20 символов).
 * @property fieldJson JSON-строка с массивом Array<Array<Int>> размером 10×10.
 * @property date      Дата-время создания "dd.MM.yyyy HH:mm".
 */
@Entity(tableName = "game_field")
data class GameField(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "field_id")
    val fieldId: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "field")
    val fieldJson: String,

    @ColumnInfo(name = "date")
    val date: String
)