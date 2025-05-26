package com.example.battleship_game.presentation.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import com.example.battleship_game.common.BaseActivity
import com.example.battleship_game.R
import com.example.battleship_game.databinding.ActivityMainBinding
import com.example.battleship_game.dialog.CustomAlertDialog
import com.example.battleship_game.presentation.help.HelpActivity
import com.example.battleship_game.presentation.profile.ProfileActivity
import com.google.android.material.snackbar.Snackbar

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Edge-to-edge отступы
        applyEdgeInsets(binding.main)

        // Скрываем SystemBars
        enterImmersiveMode()

        // Навешиваем слушатели на кнопки
        setupListeners()

        // Перехват системной кнопки «Назад»
        onBackPressedDispatcher.addCallback(this) {
            showExitConfirmDialog()
        }
    }

    private fun setupListeners() {
        binding.apply {
            btnPlay.setOnClickListener {
                //startActivity(Intent(this, GameSetupActivity::class.java))
                Snackbar.make(main, "Переход по кнопке играть", Snackbar.LENGTH_SHORT).show()
            }
            btnProfile.setOnClickListener {
                startActivity(Intent(this@MainActivity, ProfileActivity::class.java))
                Snackbar.make(main, "Переход по кнопке профиль", Snackbar.LENGTH_SHORT).show()
            }
            btnStats.setOnClickListener {
                //startActivity(Intent(this, StatsActivity::class.java))
                Snackbar.make(main, "Переход по кнопке статистика", Snackbar.LENGTH_SHORT).show()
            }
            btnHelp.setOnClickListener {
                startActivity(Intent(this@MainActivity, HelpActivity::class.java))
                Snackbar.make(main, "Переход по кнопке справка", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun showExitConfirmDialog() {
        CustomAlertDialog(this)
            .setIcon(R.drawable.ic_launcher_foreground)
            .setTitle(R.string.exit_title)
            .setMessage(R.string.exit_message)
            .setNegativeButtonText(R.string.action_cancel)
            .setPositiveButtonText(R.string.action_yes)
            .setOnPositiveClickListener {
                finishAffinity()
            }
            .show()
    }

}