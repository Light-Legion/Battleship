package com.example.battleship_game.presentation.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import com.example.battleship_game.R
import com.example.battleship_game.common.BaseActivity
import com.example.battleship_game.common.UserPreferences.isMusicEnabled
import com.example.battleship_game.databinding.ActivityMainBinding
import com.example.battleship_game.dialog.CustomAlertDialog
import com.example.battleship_game.presentation.help.HelpActivity
import com.example.battleship_game.presentation.profile.ProfileActivity
import com.example.battleship_game.presentation.setup.GameSetupActivity
import com.example.battleship_game.presentation.stats.StatsActivity
import com.example.battleship_game.services.MusicService

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

        initializeMusic()
        // Навешиваем слушатели на кнопки
        setupListeners()
        updateMusicButton()

        // Перехват системной кнопки «Назад»
        onBackPressedDispatcher.addCallback(this) {
            showExitConfirmDialog()
        }
    }

    private fun initializeMusic() {
        if (isMusicEnabled) {
            startService(Intent(this, MusicService::class.java).apply {
                action = MusicService.ACTION_PLAY
            })
        }
    }

    private fun setupListeners() {
        binding.apply {
            btnPlay.setOnClickListener {
                startActivity(Intent(this@MainActivity, GameSetupActivity::class.java))
            }
            btnProfile.setOnClickListener {
                startActivity(Intent(this@MainActivity, ProfileActivity::class.java))
            }
            btnStats.setOnClickListener {
                startActivity(Intent(this@MainActivity, StatsActivity::class.java))
            }
            btnHelp.setOnClickListener {
                startActivity(Intent(this@MainActivity, HelpActivity::class.java))
            }

            btnMusic.setOnClickListener {
                toggleMusic()
            }
        }
    }

    private fun toggleMusic() {
        isMusicEnabled = !isMusicEnabled

        val musicIntent = Intent(this, MusicService::class.java).apply {
            action = if (isMusicEnabled) {
                MusicService.ACTION_PLAY
            } else {
                MusicService.ACTION_PAUSE
            }
        }
        startService(musicIntent)

        updateMusicButton()
    }

    private fun updateMusicButton() {
        binding.btnMusic.text = if (isMusicEnabled) {
            getString(R.string.music_off)
        } else {
            getString(R.string.music_on)
        }
    }

    private fun showExitConfirmDialog() {
        CustomAlertDialog(this)
            .setIcon(R.drawable.ic_dialog_warning)
            .setTitle(R.string.exit_title)
            .setMessage(R.string.exit_message)
            .setNegativeButtonText(R.string.action_cancel)
            .setPositiveButtonText(R.string.action_yes)
            .setOnPositiveClickListener {
                finishAffinity()
            }
            .show()
    }

    override fun onResume() {
        super.onResume()
        updateMusicButton()
    }

}