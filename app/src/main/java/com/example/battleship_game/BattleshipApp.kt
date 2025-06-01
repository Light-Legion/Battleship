package com.example.battleship_game

import android.app.Application
import android.content.Intent
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.battleship_game.common.UserPreferences.isMusicEnabled
import com.example.battleship_game.data.db.AppDatabase
import com.example.battleship_game.services.MusicService

/**
 * Класс Application: инициализируем базу один раз при старте.
 */
class BattleshipApp : Application(), DefaultLifecycleObserver {
    override fun onCreate() {
        super<Application>.onCreate()
        // Проверяем и создаём базу заранее
        AppDatabase.getInstance(this)
        // Регистрируем наблюдателя за жизненным циклом приложения
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    // Когда приложение становится видимым (входит в foreground)
    override fun onStart(owner: LifecycleOwner) {
        if (isMusicEnabled) {
            startForegroundService(
                Intent(this, MusicService::class.java).apply {
                    action = MusicService.ACTION_PLAY
                }
            )
        }
    }

    // Когда приложение уходит в фон (неактивно)
    override fun onStop(owner: LifecycleOwner) {
        if (isMusicEnabled) {
            startService(
                Intent(this, MusicService::class.java).apply {
                    action = MusicService.ACTION_PAUSE
                }
            )
        }
    }
}