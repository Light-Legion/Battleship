package com.example.battleship_game.presentation.splash

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.animation.LinearInterpolator
import androidx.core.animation.doOnEnd
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.battleship_game.common.BaseActivity
import com.example.battleship_game.databinding.ActivitySplashBinding
import com.example.battleship_game.presentation.main.MainActivity

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
        val animator = ObjectAnimator.ofInt(binding.progress, "progress", 0, 100)
        animator.duration = 3000L //в миллисекундах, 1000L = 1 секунда
        animator.interpolator = LinearInterpolator() //равномерное заполнение
        animator.start()
        animator.doOnEnd { openMain() }
    }

    private fun openMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}