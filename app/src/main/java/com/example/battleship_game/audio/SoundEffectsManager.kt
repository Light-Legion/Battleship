package com.example.battleship_game.audio

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import com.example.battleship_game.R
import com.example.battleship_game.services.MusicService

/**
 * Менеджер звуковых эффектов для выстрелов в бою: попадание в корабль, промах по воде и затопление корабля.
 * Использует SoundPool для низкой задержки воспроизведения коротких эффектов.
 * Для приглушения фоновой музыки отправляет Intentы ACTION_DUCK / ACTION_UNDUCK в MusicService.
 *
 * Перед использованием убедитесь, что в res/raw присутствуют:
 * - sound_hitting_water.mp3
 * - sound_hitting_ship.mp3
 * - sound_sunk_ship.mp3
 *
 * Вызовите [release] в onDestroy() Activity/Fragment, чтобы освободить ресурсы SoundPool.
 */
class SoundEffectsManager(private val context: Context) {

    // SoundPool для эффектов
    private val soundPool: SoundPool

    // Идентификаторы звуков в SoundPool
    private val soundIdHitWater: Int
    private val soundIdHitShip: Int
    private val soundIdSunkShip: Int

    // Длительности эффектов в миллисекундах
    private val durationHitWaterMs: Int
    private val durationHitShipMs: Int
    private val durationSunkShipMs: Int

    // Счётчик активных воспроизводимых эффектов, чтобы не восстанавливать громкость слишком рано
    private var activeEffectsCount = 0

    // Handler для планирования восстановления громкости
    private val handler = Handler(Looper.getMainLooper())

    init {
        // Инициализация AudioAttributes для SoundPool
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        // Увеличиваем maxStreams до 3, чтобы поддерживать одновременное воспроизведение нескольких эффектов
        soundPool = SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(audioAttributes)
            .build()

        // Загружаем ресурсы в SoundPool. Если ресурсов нет или ошибка, приложение упадёт здесь — убедитесь, что raw файлы добавлены.
        soundIdHitWater = soundPool.load(context, R.raw.sound_hitting_water, 1)
        soundIdHitShip = soundPool.load(context, R.raw.sound_hitting_ship, 1)
        soundIdSunkShip = soundPool.load(context, R.raw.sound_sunk_ship, 1)

        // Определяем длительности эффектов: создаём временный MediaPlayer, получаем длительность, затем release()
        durationHitWaterMs = getRawSoundDurationMs(R.raw.sound_hitting_water)
        durationHitShipMs = getRawSoundDurationMs(R.raw.sound_hitting_ship)
        durationSunkShipMs = getRawSoundDurationMs(R.raw.sound_sunk_ship)
    }

    /**
     * Получает длительность звукового ресурса raw в миллисекундах.
     * Для этого создаётся временный MediaPlayer, извлекается длительность, затем освобождается.
     */
    private fun getRawSoundDurationMs(resId: Int): Int {
        return try {
            val mp = MediaPlayer.create(context, resId)
            val duration = mp?.duration ?: 0
            mp?.release()
            duration
        } catch (e: Exception) {
            // В случае ошибки возвращаем 0
            0
        }
    }

    /**
     * Воспроизведение звука попадания в корабль.
     * При этом фоновая музыка будет приглушена на время эффекта.
     */
    fun playHitSound() {
        playSoundWithDuck(soundIdHitShip, durationHitShipMs)
    }

    /**
     * Воспроизведение звука попадания в воду (промаха).
     * При этом фоновая музыка будет приглушена на время эффекта.
     */
    fun playMissSound() {
        playSoundWithDuck(soundIdHitWater, durationHitWaterMs)
    }

    /**
     * Воспроизведение звука затопления корабля.
     * Длительность обычно больше (например, ~3000 мс). При этом фон приглушается на время эффекта.
     */
    fun playSunkSound() {
        playSoundWithDuck(soundIdSunkShip, durationSunkShipMs)
    }

    /**
     * Освобождение ресурсов SoundPool. Вызывать в onDestroy() Activity/Fragment.
     */
    fun release() {
        soundPool.release()
        handler.removeCallbacksAndMessages(null)
    }

    /**
     * Внутренний метод: воспроизводит звук из SoundPool, при этом:
     * - Отправляет Intent ACTION_DUCK в MusicService и увеличивает activeEffectsCount.
     * - Планирует Runnable через durationMs для уменьшения activeEffectsCount и, при необходимости, отправки ACTION_UNDUCK.
     *
     * @param soundId идентификатор звука в SoundPool
     * @param durationMs длительность эффекта в миллисекундах (если 0 или отрицательное, восстановление будет немедленным)
     */
    private fun playSoundWithDuck(soundId: Int, durationMs: Int) {
        // Увеличиваем счётчик активных эффектов
        activeEffectsCount++
        // Отправляем Intent для приглушения фоновой музыки
        sendMusicServiceAction(MusicService.ACTION_DUCK)

        // Проигрываем звук. Уровень громкости эффекта = 1.0f для левого и правого каналов.
        soundPool.play(soundId, 1f, 1f, /*priority=*/1, /*loop=*/0, /*rate=*/1f)

        // Планируем восстановление громкости по окончании эффекта
        if (durationMs > 0) {
            handler.postDelayed({
                activeEffectsCount = (activeEffectsCount - 1).coerceAtLeast(0)
                if (activeEffectsCount == 0) {
                    // Если больше нет активных эффектов — восстанавливаем громкость
                    sendMusicServiceAction(MusicService.ACTION_UNDUCK)
                }
            }, durationMs.toLong())
        } else {
            // Если длительность не определена, немедленно уменьшаем счётчик и восстанавливаем
            activeEffectsCount = (activeEffectsCount - 1).coerceAtLeast(0)
            if (activeEffectsCount == 0) {
                sendMusicServiceAction(MusicService.ACTION_UNDUCK)
            }
        }
    }

    /**
     * Отправляет Intent в MusicService для управления громкостью фоновой музыки.
     * Используется startService, чтобы сервис получил onStartCommand с нужным action.
     */
    private fun sendMusicServiceAction(action: String) {
        // Используем applicationContext, чтобы не привязываться к Activity
        val intent = Intent(context, MusicService::class.java).apply {
            this.action = action
        }
        // Запуск сервиса; если сервис ещё не запущен, он будет создан, но если action = DUCK/UNDUCK,
        // а плеер не запущен, MusicService просто проигнорирует (setVolume вызов на null).
        context.startService(intent)
    }
}
