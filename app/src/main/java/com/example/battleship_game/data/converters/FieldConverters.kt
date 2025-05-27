package com.example.battleship_game.data.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Конвертер JSON ↔ List<List<Int>> (10×10 поле).
 */
class FieldConverters {
    private val gson = Gson()
    private val type = object : TypeToken<List<List<Int>>>() {}.type

    /** Десериализует JSON в список списков. */
    @TypeConverter
    fun fromJson(json: String): List<List<Int>> =
        gson.fromJson(json, type)

    /** Сериализует поле в JSON-строку. */
    @TypeConverter
    fun toJson(field: List<List<Int>>): String =
        gson.toJson(field)
}