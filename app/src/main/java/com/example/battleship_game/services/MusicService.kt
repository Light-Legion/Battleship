package com.example.battleship_game.services

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.IBinder
import androidx.annotation.RawRes
import com.example.battleship_game.R

/**
 * MusicService - сервис для фонового воспроизведения музыки в приложении.
 *
 * Особенности:
 * - Поддерживает плейлист из нескольких треков
 * - Автоматически переходит к следующему треку после окончания текущего
 * - Циклическое воспроизведение всего плейлиста
 * - Поддерживает команды воспроизведения, паузы и остановки
 *
 * Управление осуществляется через Intent с действиями:
 * - ACTION_PLAY: начать/возобновить воспроизведение
 * - ACTION_PAUSE: приостановить воспроизведение
 * - ACTION_STOP: полностью остановить воспроизведение и сервис
 */
class MusicService : Service() {

    companion object {
        /** Действие: начать или возобновить воспроизведение */
        const val ACTION_PLAY = "com.example.battleship_game.services.action.PLAY"

        /** Действие: приостановить воспроизведение */
        const val ACTION_PAUSE = "com.example.battleship_game.services.action.PAUSE"

        /** Действие: полностью остановить воспроизведение и сервис */
        const val ACTION_STOP = "com.example.battleship_game.services.action.STOP"
    }

    /**
     * Плейлист: список идентификаторов аудиоресурсов.
     * Добавляйте сюда все фоновые треки для циклического воспроизведения.
     */
    @RawRes
    private val playlist = listOf(
        R.raw.background_music_1,
        R.raw.background_music_2,
        R.raw.background_music_3
    )

    /**
     * Индекс текущего трека в плейлисте.
     * Автоматически обновляется при переходе к следующему треку.
     */
    private var currentTrackIndex: Int = 0

    /**
     * Экземпляр MediaPlayer для управления воспроизведением.
     * Инициализируется при создании сервиса и при переходе к новому треку.
     */
    private var mediaPlayer: MediaPlayer? = null

    /**
     * Флаг, указывающий, находится ли воспроизведение на паузе.
     * Используется для корректного возобновления воспроизведения.
     */
    private var isPaused: Boolean = false

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Вызывается при создании сервиса.
     * Инициализирует MediaPlayer для первого трека в плейлисте.
     */
    override fun onCreate() {
        super.onCreate()
        initializeMediaPlayer()
    }

    /**
     * Инициализирует MediaPlayer для текущего трека.
     * Настраивает обработчик завершения трека для автоматического перехода к следующему.
     */
    private fun initializeMediaPlayer() {
        mediaPlayer = MediaPlayer.create(this, playlist[currentTrackIndex]).apply {
            // Настраиваем переход к следующему треку по завершении текущего
            setOnCompletionListener { nextTrack() }

            // Отключаем зацикливание отдельного трека (циклим весь плейлист)
            isLooping = false

            // Устанавливаем громкость (70% от максимальной)
            setVolume(0.25f, 0.25f)
        }
    }

    /**
     * Обрабатывает входящие команды для управления воспроизведением.
     *
     * @param intent Intent с действием (ACTION_PLAY, ACTION_PAUSE или ACTION_STOP)
     * @param flags Флаги запуска
     * @param startId Идентификатор запуска
     * @return Режим работы сервиса (START_STICKY для автоматического перезапуска)
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Обрабатываем действие из Intent
        when (intent?.action) {
            ACTION_PLAY -> play()
            ACTION_PAUSE -> pause()
            ACTION_STOP -> stop()
        }

        // Возвращаем START_STICKY для автоматического перезапуска сервиса
        return START_STICKY
    }

    /**
     * Начинает или возобновляет воспроизведение музыки.
     *
     * - Если медиаплеер не инициализирован, создает новый
     * - Если воспроизведение на паузе, возобновляет с текущей позиции
     * - Если ничего не играет, начинает текущий трек
     */
    private fun play() {
        mediaPlayer?.let { player ->
            when {
                // Возобновляем с паузы
                isPaused -> {
                    player.start()
                    isPaused = false
                }

                // Начинаем текущий трек (если не играет)
                !player.isPlaying -> player.start()
            }
        } ?: run {
            // Инициализируем и запускаем, если плеер не создан
            initializeMediaPlayer()
            mediaPlayer?.start()
        }
    }

    /**
     * Приостанавливает воспроизведение музыки.
     * Устанавливает флаг isPaused в true.
     */
    private fun pause() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
                isPaused = true
            }
        }
    }

    /**
     * Полностью останавливает воспроизведение и освобождает ресурсы.
     * Вызывает остановку сервиса.
     */
    private fun stop() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.stop()
            }
            player.release()
        }
        mediaPlayer = null
        isPaused = false

        // Останавливаем сервис
        stopSelf()
    }

    /**
     * Переходит к следующему треку в плейлисте.
     *
     * 1. Освобождает текущий MediaPlayer
     * 2. Увеличивает индекс текущего трека (с возвратом к 0 в конце плейлиста)
     * 3. Инициализирует новый MediaPlayer для следующего трека
     * 4. Начинает воспроизведение
     */
    private fun nextTrack() {
        // Освобождаем ресурсы текущего плеера
        mediaPlayer?.release()

        // Переходим к следующему треку (с цикличностью)
        currentTrackIndex = (currentTrackIndex + 1) % playlist.size

        // Инициализируем плеер для нового трека
        initializeMediaPlayer()

        // Начинаем воспроизведение
        mediaPlayer?.start()
    }

    /**
     * Вызывается при уничтожении сервиса.
     * Гарантирует освобождение ресурсов MediaPlayer.
     */
    override fun onDestroy() {
        stop()
        super.onDestroy()
    }
}