package com.example.battleship_game.data.converters

import androidx.room.TypeConverter
import com.example.battleship_game.data.model.ShipPlacement
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Конвертер List<ShipPlacement> ↔ JSON для Room.
 */
class PlacementConverters {
    private val gson = Gson()
    private val type = object : TypeToken<List<ShipPlacement>>() {}.type

    @TypeConverter
    fun fromList(list: List<ShipPlacement>): String = gson.toJson(list)

    @TypeConverter
    fun toList(json: String): List<ShipPlacement> =
        gson.fromJson(json, type)
}