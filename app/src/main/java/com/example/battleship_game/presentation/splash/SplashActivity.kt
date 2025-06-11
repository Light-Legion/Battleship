package com.example.battleship_game.presentation.splash

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.animation.LinearInterpolator
import androidx.activity.addCallback
import androidx.core.animation.doOnEnd
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.battleship_game.common.BaseActivity
import com.example.battleship_game.common.UserPreferences.clearPendingGameFlag
import com.example.battleship_game.common.UserPreferences.nickname
import com.example.battleship_game.common.UserPreferences.pendingGameDifficulty
import com.example.battleship_game.common.UserPreferences.pendingGameStartTime
import com.example.battleship_game.data.db.AppDatabase
import com.example.battleship_game.data.entity.GameHistory
import com.example.battleship_game.data.model.Difficulty
import com.example.battleship_game.data.model.GameResult
import com.example.battleship_game.databinding.ActivitySplashBinding
import com.example.battleship_game.presentation.main.MainActivity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity() {

    private lateinit var binding: ActivitySplashBinding

    // Хранит Job вставки, чтобы можно было дождаться
    private var insertJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyEdgeInsets(binding.main)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Запускаем вставку параллельно
        insertJob = lifecycleScope.launch(Dispatchers.IO) {
            insertPendingGameIfAny()
        }

        simulateLoading()

        onBackPressedDispatcher.addCallback(this) {
            finish()
        }
    }

    /**
     * Проверяет флаг незавершённой игры и, если он есть, вставляет поражение в БД.
     * Вызывается в Dispatchers.IO корутине.
     */
    private suspend fun insertPendingGameIfAny() {
        val pendingStart = pendingGameStartTime
        val pendingDiffName = pendingGameDifficulty
        if (pendingStart != 0L && !pendingDiffName.isNullOrEmpty()) {
            val playerName = nickname
            val difficulty = try {
                Difficulty.valueOf(pendingDiffName)
            } catch (e: Exception) {
                Difficulty.MEDIUM
            }
            val now = LocalDateTime.now()
            val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
            val dateStr = now.format(formatter)
            val gameHistory = GameHistory(
                name = playerName,
                result = GameResult.LOSS,
                level = difficulty,
                date = dateStr
            )
            try {
                AppDatabase.getInstance(applicationContext).gameHistoryDao().insert(gameHistory)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                clearPendingGameFlag()
            }
        }
    }

    private fun simulateLoading() {
        val animator = ObjectAnimator.ofInt(binding.progress, "progress", 0, 100)
        animator.duration = 3000L //в миллисекундах, 1000L = 1 секунда
        animator.interpolator = LinearInterpolator() //равномерное заполнение
        animator.start()
        animator.doOnEnd {
            lifecycleScope.launch {
                insertJob?.let { job ->
                    if (job.isActive) {
                        try {
                            job.join()
                        } catch (_: CancellationException) {
                            // Игнорируем, если корутина была отменена
                        } catch (e: Exception) {
                            // Любые прочие ошибки при ожидании можно залогировать
                            e.printStackTrace()
                        }
                    }
                }
                // 4) После ожидания вставки (или если её не было): переходим в главный экран
                openMain()
            }
        }
    }

    private fun openMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}