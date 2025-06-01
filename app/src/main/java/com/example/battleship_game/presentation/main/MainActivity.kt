package com.example.battleship_game.presentation.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.addCallback
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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

    companion object {
        private const val NOTIFICATION_PERMISSION_CODE = 1001
    }

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

        checkNotificationPermission()
        initializeMusic()
        updateMusicButton()

        // Перехват системной кнопки «Назад»
        onBackPressedDispatcher.addCallback(this) {
            showExitConfirmDialog()
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

    private fun initializeMusic() {
        if (isMusicEnabled) {
            startMusicService(MusicService.ACTION_PLAY)
        }
    }

    private fun toggleMusic() {
        isMusicEnabled = !isMusicEnabled

        startMusicService(
            if (isMusicEnabled) MusicService.ACTION_PLAY
            else MusicService.ACTION_PAUSE
        )

        updateMusicButton()
    }

    private fun startMusicService(action: String) {
        startForegroundService(
            Intent(this, MusicService::class.java).apply {
                this.action = action
            }
        )
    }

    private fun updateMusicButton() {
        binding.btnMusic.setImageResource(
            if (isMusicEnabled) R.drawable.ic_btn_music_on else R.drawable.ic_btn_music_off
        )
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_CODE
                )
            }
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