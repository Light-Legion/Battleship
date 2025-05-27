package com.example.battleship_game.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.battleship_game.data.converters.Converters
import com.example.battleship_game.data.converters.FieldConverters
import com.example.battleship_game.data.dao.GameFieldDao
import com.example.battleship_game.data.dao.GameProgressDao
import com.example.battleship_game.data.entity.GameField
import com.example.battleship_game.data.entity.GameProgress

/**
 * Основной класс базы данных Room.
 */
@Database(
    entities = [GameProgress::class, GameField::class],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class, FieldConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameProgressDao(): GameProgressDao
    abstract fun gameFieldDao(): GameFieldDao

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