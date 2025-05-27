package com.example.battleship_game.presentation.splash

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.animation.LinearInterpolator
import androidx.core.animation.doOnEnd
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.battleship_game.common.BaseActivity
import com.example.battleship_game.data.db.AppDatabase
import com.example.battleship_game.data.model.Difficulty
import com.example.battleship_game.data.entity.GameHistory
import com.example.battleship_game.data.model.GameResult
import com.example.battleship_game.databinding.ActivitySplashBinding
import com.example.battleship_game.presentation.main.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity() {

    private lateinit var binding: ActivitySplashBinding

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

        simulateLoading()
    }

    private fun simulateLoading() {
        // 1. Добавим 5 тестовых записей
        lifecycleScope.launch(Dispatchers.IO) {
            val dao = AppDatabase.getInstance(this@SplashActivity).gameHistoryDao()
            val now = LocalDateTime.now()
            val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

            val testData = listOf(
                GameHistory(name = "Test", result = GameResult.WIN,  level = Difficulty.EASY,   date = now.format(formatter)),
                GameHistory(name = "Test", result = GameResult.LOSS, level = Difficulty.MEDIUM, date = now.format(formatter)),
                GameHistory(name = "Test", result = GameResult.WIN,  level = Difficulty.HARD,   date = now.format(formatter)),
                GameHistory(name = "Test", result = GameResult.LOSS, level = Difficulty.EASY,   date = now.format(formatter)),
                GameHistory(name = "Test", result = GameResult.WIN,  level = Difficulty.MEDIUM, date = now.format(formatter))
            )

            testData.forEach { dao.insert(it) }
        }

        val animator = ObjectAnimator.ofInt(binding.progress, "progress", 0, 100)
        animator.duration = 1000L
        animator.interpolator = LinearInterpolator() //равномерное заполнение
        animator.start()
        animator.doOnEnd { openMain() }
    }

    private fun openMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}