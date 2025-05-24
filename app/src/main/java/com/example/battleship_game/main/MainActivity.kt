package com.example.battleship_game.main

import android.os.Bundle
import android.view.Gravity
import android.widget.Toast
import androidx.activity.addCallback
import com.example.battleship_game.BaseActivity
import com.example.battleship_game.R
import com.example.battleship_game.databinding.ActivityMainBinding
import com.example.battleship_game.dialog.CustomAlertDialog

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
                toastShowMessage("Переход по кнопке играть")
            }
            btnProfile.setOnClickListener {
                //startActivity(Intent(this, ProfileActivity::class.java))
                toastShowMessage("Переход по кнопке профиль")
            }
            btnStats.setOnClickListener {
                //startActivity(Intent(this, StatsActivity::class.java))
                toastShowMessage("Переход по кнопке статистика")
            }
            btnHelp.setOnClickListener {
                //startActivity(Intent(this, HelpActivity::class.java))
                toastShowMessage("Переход по кнопке справка")
            }
        }
    }

    //Функция для вывода на экран сообщения-тоста
    private fun toastShowMessage(message: String){
        val toast = Toast.makeText(this, message, Toast.LENGTH_SHORT)
        toast.setGravity(Gravity.TOP, 0, 0)
        toast.show()
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