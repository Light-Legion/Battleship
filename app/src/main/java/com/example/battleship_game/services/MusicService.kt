package com.example.battleship_game.services

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.IBinder
import androidx.annotation.RawRes
import com.example.battleship_game.R

/**
 * MusicService — простой (не-лоченный) сервис для фонового проигрывания музыки.
 *
 * Сервис управляется через ACTION-поля Intent:
 *  • ACTION_PLAY  — начать (или продолжить) проигрывание плейлиста с текущего трека
 *  • ACTION_STOP  — остановить проигрывание и завершить сервис
 *
 * Внутри сервис хранит список музыкальных треков (ресурсов raw) и проигрывает их
 * по очереди. Когда последний трек заканчивается, начинается первый (зацикливание).
 */
class MusicService : Service() {

    companion object {
        /** Действие для Intent при запуске музыки */
        const val ACTION_PLAY = "com.example.battleship_game.services.action.PLAY"
        /** Действие для Intent при остановке музыки */
        const val ACTION_STOP = "com.example.battleship_game.services.action.STOP"
    }

    /** Список идентификаторов музыкальных файлов (из res/raw) */
    @RawRes
    private val playlist = listOf(
        R.raw.background_music_1,
        R.raw.background_music_2,
        R.raw.background_music_3
    )

    /** Индекс текущего трека в плейлисте */
    private var currentTrackIndex: Int = 0

    /** Экземпляр MediaPlayer для проигрывания текущего трека */
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate() {
        super.onCreate()
        // Здесь мы не инициализируем mediaPlayer — это делается при ACTION_PLAY
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Проверяем, что Intent не null, считываем action
        intent?.action?.let { action ->
            when (action) {
                ACTION_PLAY -> handleActionPlay()
                ACTION_STOP -> handleActionStop()
            }
        }
        // Возвращаем START_NOT_STICKY, чтобы система не перезапускала сервис автоматически
        return START_NOT_STICKY
    }

    /**
     * Обработка команды PLAY.
     * Если mediaPlayer еще не создан, создаём его и запускаем первый (или текущий) трек.
     * Если mediaPlayer создан, но музыка на паузе, просто возобновляем.
     */
    private fun handleActionPlay() {
        if (mediaPlayer == null) {
            // Создаём новый MediaPlayer для текущего трека
            mediaPlayer = MediaPlayer.create(this, playlist[currentTrackIndex]).apply {
                isLooping = false // Зацикливать будем вручную, по окончанию трека
                setOnCompletionListener {
                    // Как только текущий трек заканчивается:
                    onTrackCompletion()
                }
                start()
            }
        } else {
            // Если mediaPlayer уже есть, просто проверим и запустим, если он на паузе
            mediaPlayer?.let { player ->
                if (!player.isPlaying) {
                    player.start()
                }
            }
        }
    }

    /**
     * Действия, которые нужно выполнить, когда текущий трек доиграл до конца:
     *  - Увеличить индекс до следующего (с учётом цикличности)
     *  - Освободить предыдущий mediaPlayer
     *  - Создать и запустить новый mediaPlayer для следующего трека
     */
    private fun onTrackCompletion() {
        // Сдвигаем индекс к следующему треку; если дошли до конца, возвращаемся к нулю
        currentTrackIndex = (currentTrackIndex + 1) % playlist.size

        // Освобождаем предыдущий player
        mediaPlayer?.release()
        mediaPlayer = null

        // Создаём новый player для следующего трека
        mediaPlayer = MediaPlayer.create(this, playlist[currentTrackIndex]).apply {
            isLooping = false
            setOnCompletionListener {
                onTrackCompletion()
            }
            start()
        }
    }

    /**
     * Остановка проигрывания и завершение сервиса.
     */
    private fun handleActionStop() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.stop()
            }
            player.release()
        }
        mediaPlayer = null

        // Завершаем работу сервиса
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Если сервис уничтожается (например, приложение свернулось), освобождаем ресурсы
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.stop()
            }
            player.release()
        }
        mediaPlayer = null
    }

    override fun onBind(intent: Intent?): IBinder? {
        // Поскольку это «простой» сервис (не биндовый), возвращаем null
        return null
    }
}