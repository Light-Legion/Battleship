package com.example.battleship_game.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.IBinder
import androidx.annotation.RawRes
import androidx.core.app.NotificationCompat
import com.example.battleship_game.R
import com.example.battleship_game.common.UserPreferences.lastTrackIndex
import com.example.battleship_game.presentation.main.MainActivity

/**
 * MusicService — foreground-сервис для фонового воспроизведения музыки.
 *
 * Он поддерживает несколько треков (плейлист), автоматически переключается на следующий трек
 * после окончания текущего и зацикливает весь плейлист.
 *
 * Управление происходит через Intent.action:
 *  • ACTION_PLAY  — начать или возобновить воспроизведение
 *  • ACTION_PAUSE — приостановить воспроизведение (оставить сервис запущенным)
 *  • ACTION_STOP  — полностью остановить воспроизведение и завершить сервис
 *
 * Сервис запрашивает аудиофокус, чтобы проигрывать музыку корректно, и освобождает фокус при паузе/остановке.
 */
class MusicService : Service(), AudioManager.OnAudioFocusChangeListener {

    companion object {
        /** Действие: начать или возобновить воспроизведение */
        const val ACTION_PLAY = "com.example.battleship_game.services.action.PLAY"

        /** Действие: приостановить воспроизведение */
        const val ACTION_PAUSE = "com.example.battleship_game.services.action.PAUSE"

        /** Действие: полностью остановить воспроизведение и сервис */
        const val ACTION_STOP = "com.example.battleship_game.services.action.STOP"

        /** Идентификатор канала уведомлений для foreground-сервиса */
        const val NOTIFICATION_CHANNEL_ID = "music_service_channel"

        /** ID уведомления (может быть любым константным целым числом) */
        const val NOTIFICATION_ID = 1
    }

    /**
     * Плейлист: список raw-ресурсов для фоновых треков.
     * Положите в res/raw файлы background_music_1.mp3, background_music_2.mp3, background_music_3.mp3.
     */
    @RawRes
    private val playlist = listOf(
        /*R.raw.background_music_1,
        R.raw.background_music_2,
        R.raw.background_music_3,*/
        R.raw.background_music_welost,
        R.raw.background_music_forgottenbattlefied_dividedswords,
        R.raw.background_music_falling_leaves_autumns_brush,
        R.raw.background_music_flyingwaters_rainfromtheground,
        R.raw.background_music_springmeadows_battlingbreeze,
        R.raw.background_music_springmeadows_beneaththebluetree,
        R.raw.background_music_springmeadows_getupforlumiere,
        R.raw.background_music_stonewavecliffs_wardingblades,
        R.raw.background_music_worldmap_takingdownthepaintress,
        R.raw.background_music_worldmap_untilyouregone
    )

    /** Индекс текущего трека в плейлисте */
    private var currentTrackIndex: Int = 0

    /** Экземпляр MediaPlayer для воспроизведения */
    private var mediaPlayer: MediaPlayer? = null

    /** Флаг, указывающий, поставлена ли музыка на паузу (нужно ли возобновить) */
    private var isPaused: Boolean = false

    /** AudioManager для работы с аудиофокусом */
    private lateinit var audioManager: AudioManager

    /** Флаг, указывающий, переведён ли сервис в foreground-режим */
    private var isForegroundService: Boolean = false

    //----------------------------------------------------------------------------------------------
    // 1. Жизненный цикл: onCreate, onStartCommand, onDestroy
    //----------------------------------------------------------------------------------------------

    override fun onCreate() {
        super.onCreate()

        // Инициализируем AudioManager
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        // Создаём канал уведомлений (нужно для Android 8.0+)
        createNotificationChannel()

        currentTrackIndex = lastTrackIndex

        // Инициализируем MediaPlayer для первого трека (но ещё не запускаем)
        initializeMediaPlayer()
    }

    /**
     * Обработка входящих Intent: определяем action и вызываем нужный метод.
     * Возвращаем START_STICKY, чтобы система перезапускала сервис, если его убьют.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY  -> play()
            ACTION_PAUSE -> pause()
            ACTION_STOP  -> stopServiceAndRelease()
        }
        return START_STICKY
    }

    /**
     * Уничтожаем сервис — освобождаем ресурсы MediaPlayer и аудиофокус.
     */
    override fun onDestroy() {
        // При уничтожении убеждаемся, что всё корректно остановлено
        try {
            stopServiceAndRelease()
        } finally {
            super.onDestroy()
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        // Этот сервис не поддерживает биндинг (bindService), поэтому возвращаем null
        return null
    }

    //----------------------------------------------------------------------------------------------
    // 2. Инициализация MediaPlayer и переход к следующему треку
    //----------------------------------------------------------------------------------------------

    /**
     * Создаёт или перестраивает MediaPlayer для текущего трека из playlist[currentTrackIndex].
     * Устанавливает OnCompletionListener, чтобы автоматически переходить к nextTrack().
     */
    private fun initializeMediaPlayer() {
        try {
            // Если ранее был создан MediaPlayer, освобождаем его
            mediaPlayer?.release()

            val newPlayer = MediaPlayer.create(this, playlist[currentTrackIndex])
            if (newPlayer == null) {
                nextTrack() // Пропустить битый трек
                return
            }

            mediaPlayer = newPlayer.apply {
                isLooping = false
                setVolume(0.5f, 0.5f)
                setOnCompletionListener { nextTrack() }
            }
        } catch (_: Exception) {
            nextTrack()
        }
    }

    /**
     * Переходит к следующему треку плейлиста (с учётом цикличности) и сразу начинает воспроизведение.
     */
    private fun nextTrack() {
        // Освобождаем текущий плеер
        mediaPlayer?.run {
            stop()
            release()
        }
        mediaPlayer = null

        // Считаем индекс следующего трека (если на последнем — возвращаемся к 0)
        currentTrackIndex = (currentTrackIndex + 1) % playlist.size

        // Инициализируем плеер заново для следующего трека
        initializeMediaPlayer()

        // Запускаем новый трек
        mediaPlayer?.start()
        startForegroundServiceWithNotification()
    }

    //----------------------------------------------------------------------------------------------
    // 3. Работа с аудиофокусом (AudioManager.OnAudioFocusChangeListener)
    //----------------------------------------------------------------------------------------------

    /**
     * Запрашивает аудиофокус у системы. Возвращает true, если фокус получен.
     */
    private fun requestAudioFocus(): Boolean {
        val result = audioManager.requestAudioFocus(
            this,                           // Наш MusicService реализует OnAudioFocusChangeListener
            AudioManager.STREAM_MUSIC,      // Тип потока
            AudioManager.AUDIOFOCUS_GAIN    // Хотим постоянный фокус для музыки
        )
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    /**
     * Освобождает аудиофокус, когда музыка приостановлена или остановлена.
     */
    private fun abandonAudioFocus() {
        audioManager.abandonAudioFocus(this)
    }

    /**
     * Этот метод будет вызван, когда система забирает или даёт обратно аудиофокус:
     *  • AUDIOFOCUS_LOSS            — потеряли фокус надолго → приостанавливаем
     *  • AUDIOFOCUS_LOSS_TRANSIENT  — временная потеря (уведомление) → приостанавливаем
     *  • AUDIOFOCUS_GAIN            — вернули фокус → возобновляем (если не на паузе)
     */
    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // При любой потере аудиофокуса приостанавливаем трек
                pause()
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                // Вернули аудиофокус — если ранее не был на паузе, возобновляем
                if (!isPaused) play()
            }
        }
    }

    //----------------------------------------------------------------------------------------------
    // 4. Методы управления воспроизведением: play(), pause(), stopServiceAndRelease()
    //----------------------------------------------------------------------------------------------

    /**
     * Начинает или возобновляет воспроизведение.
     * 1) Запрашивает аудиофокус
     * 2) Если mediaPlayer ещё не создан — инициализирует его и стартует
     * 3) Если создан, но стоит на паузе — просто стартует
     * 4) Переводит сервис в foreground-режим с уведомлением
     */
    private fun play() {
        // 1) Просим аудиофокус
        if (!requestAudioFocus()) {
            // Если фокус не дали, не начинаем воспроизведение
            return
        }

        mediaPlayer?.let { player ->
            if (!player.isPlaying) {
                if (isPaused) {
                    // Если ранее была пауза — просто стартуем
                    player.start()
                    isPaused = false
                } else {
                    // Если плеер создан, но ещё не начинал, просто стартуем
                    player.start()
                }
            }
        } ?: run {
            // Если mediaPlayer == null (никогда не создавался) — инициализируем и стартуем
            initializeMediaPlayer()
            mediaPlayer?.start()
        }

        // Переводим сервис в foreground-режим (Android 8.0+)
        startForegroundServiceWithNotification()
    }

    /**
     * Приостанавливает воспроизведение:
     * 1) Если сейчас играет — вызывем pause()
     * 2) Устанавливаем флаг isPaused = true
     * 3) Освобождаем аудиофокус
     * 4) Выводим сервис из foreground-режима (убираем уведомление)
     */
    private fun pause() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
                isPaused = true

                // Выводим из foreground (убираем уведомление)
                stopForegroundService()
            }
        }
    }

    /**
     * Полностью останавливает плеер и освобождает ресурсы:
     * 1) Если плеер играет — останавливаем
     * 2) Вызываем release()
     * 3) Сбрасываем mediaPlayer = null; isPaused = false
     * 4) Освобождаем аудиофокус
     * 5) Убираем из foreground
     * 6) Сам сервис вызывает stopSelf()
     */
    private fun stopServiceAndRelease() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.stop()
            }
            player.release()
        }
        mediaPlayer = null
        isPaused = false

        abandonAudioFocus()
        stopForegroundService()

        stopSelf()
    }

    //----------------------------------------------------------------------------------------------
    // 5. Foreground-режим: создание канала уведомлений и самого уведомления
    //----------------------------------------------------------------------------------------------

    /**
     * Создаёт NotificationChannel, необходимый для Android 8.0+.
     */
    private fun createNotificationChannel() {
        val channelName = getString(R.string.notification_channel_name)
        val channelDescription = getString(R.string.notification_channel_description)

        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            channelName,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = channelDescription
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Создаёт Notification для foreground-сервиса.
     * Здесь вы можете задать свой layout, иконку и тексты.
     */
    private fun createNotification(): Notification {
        // Intent для открытия MainActivity
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        // Флаги с учетом версии Android
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            flags
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_btn_music_on)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(Notification.CATEGORY_SERVICE) // Важно для Android 14+
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Важно для Android 14+
            .setOngoing(true) // Постоянное уведомление
            .build()
    }

    /**
     * Переводит сервис в foreground, если он ещё не был запущен в этом режиме.
     */
    private fun startForegroundServiceWithNotification() {
        // startForeground с ID и Notification
        startForeground(NOTIFICATION_ID, createNotification())
        isForegroundService = true
    }

    /**
     * Выводит сервис из foreground-режима (убирает уведомление).
     */
    private fun stopForegroundService() {
        if (isForegroundService) {
            stopForeground(true)
            isForegroundService = false
        }
    }
}
