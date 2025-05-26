package com.example.battleship_game

import android.app.Application
import com.example.battleship_game.data.db.AppDatabase

/**
 * Класс Application: инициализируем базу один раз при старте.
 */
class BattleshipApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Проверяем и создаём базу заранее
        AppDatabase.getInstance(this)
    }
}