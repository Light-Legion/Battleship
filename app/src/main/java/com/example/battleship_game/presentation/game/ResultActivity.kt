package com.example.battleship_game.presentation.game

import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.battleship_game.R
import com.example.battleship_game.common.BaseActivity
import com.example.battleship_game.common.UserPreferences.battleDifficulty
import com.example.battleship_game.common.UserPreferences.nickname
import com.example.battleship_game.data.model.GameResult
import com.example.battleship_game.data.model.ShipPlacement
import com.example.battleship_game.databinding.ActivityResultBinding
import com.example.battleship_game.presentation.main.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ResultActivity : BaseActivity() {

    private lateinit var binding: ActivityResultBinding
    private val viewModel: ResultActivityViewModel by viewModels()

    private val ships: List<ShipPlacement> by lazy { parseShips() }
    private val result: GameResult by lazy { parseResult() }

    companion object {
        const val EXTRA_PLAYER_SHIPS = "EXTRA_PLAYER_SHIPS"
        const val EXTRA_PLAYER_RESULT = "EXTRA_PLAYER_RESULT"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // edge-to-edge + immersive
        applyEdgeInsets(binding.main)
        enterImmersiveMode()

        setupUI()
        saveGameRecord()
    }

    private fun parseShips() = intent
        .getParcelableArrayListExtra<ShipPlacement>(EXTRA_PLAYER_SHIPS)
        .orEmpty()

    private fun parseResult(): GameResult {
        return (intent.getSerializableExtra(EXTRA_PLAYER_RESULT) as? GameResult) ?: GameResult.LOSS
    }

    /**
     * Настраивает пользовательский интерфейс: фон экрана, текст результата,
     * а также обработчики кнопок «Главное меню» и «Сыграть снова».
     */
    private fun setupUI() {
        binding.apply {
            var background =
                if (result == GameResult.WIN) {
                    R.drawable.bg_screen_ships_at_sea
                } else {
                    R.drawable.bg_screen_gradient
                }
            main.setBackgroundResource(background)

            tvResult.text = result.toDisplayString()

            btnMainMenu.setOnClickListener {
                navigateToMain()
            }

            btnPlayAgain.setOnClickListener {
                navigateToLoadingWithShips()
            }
        }

        onBackPressedDispatcher.addCallback(this) {
            navigateToMain()
        }
    }

    private fun saveGameRecord() {
        lifecycleScope.launch(Dispatchers.IO) {
            val name = nickname
            val level = battleDifficulty
            val now = LocalDateTime.now()
            val date = now.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))

            viewModel.saveGameHistory(name, result, level, date)
        }
    }

    /**
     * Запускает MainActivity и завершает эту активность.
     */
    private fun navigateToMain() {
        startActivity(Intent(this@ResultActivity, MainActivity::class.java))
        finish()
    }

    /**
     * Запускает LoadingActivity, передавая туда список ships.
     */
    private fun navigateToLoadingWithShips() {
        val intent = Intent(this@ResultActivity, LoadingActivity::class.java)
            .putParcelableArrayListExtra(
                LoadingActivity.EXTRA_PLAYER_SHIPS,
                ArrayList(ships)
            )
        startActivity(intent)
        finish()
    }
}