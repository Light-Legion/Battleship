package com.example.battleship_game.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.battleship_game.data.converters.EnumConverters
import com.example.battleship_game.data.converters.PlacementConverters
import com.example.battleship_game.data.dao.GamePlacementDao
import com.example.battleship_game.data.dao.GameHistoryDao
import com.example.battleship_game.data.entity.GamePlacement
import com.example.battleship_game.data.entity.GameHistory

/**
 * Основной класс базы данных Room.
 */
@Database(
    entities = [GameHistory::class, GamePlacement::class],
    version = 2,
    exportSchema = true
)
@TypeConverters(EnumConverters::class, PlacementConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameHistoryDao(): GameHistoryDao
    abstract fun gamePlacementDao(): GamePlacementDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        /**
         * Получить синглтон-экземпляр базы.
         */
        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "battleship.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}