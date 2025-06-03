package com.example.battleship_game.presentation.result

import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.battleship_game.R
import com.example.battleship_game.common.BaseActivity
import com.example.battleship_game.common.UserPreferences.nickname
import com.example.battleship_game.data.model.Difficulty
import com.example.battleship_game.data.model.GameResult
import com.example.battleship_game.data.model.ShipPlacement
import com.example.battleship_game.databinding.ActivityResultBinding
import com.example.battleship_game.presentation.loading.LoadingActivity
import com.example.battleship_game.presentation.result.ResultActivityViewModel
import com.example.battleship_game.presentation.main.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ResultActivity : BaseActivity() {

    private lateinit var binding: ActivityResultBinding
    private val viewModel: ResultActivityViewModel by viewModels()

    companion object {
        const val EXTRA_PLAYER_SHIPS = "EXTRA_PLAYER_SHIPS"
        const val EXTRA_PLAYER_RESULT = "EXTRA_PLAYER_RESULT"
        const val EXTRA_DIFFICULTY = "EXTRA_DIFFICULTY"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // edge-to-edge + immersive
        applyEdgeInsets(binding.main)
        enterImmersiveMode()

        parseIntentExtras()
        setupUI()
        saveGameRecord()
    }

    private fun parseIntentExtras() {
        viewModel.ships = intent
            .getParcelableArrayListExtra<ShipPlacement>(EXTRA_PLAYER_SHIPS)
            .orEmpty()

        viewModel.result = intent.getSerializableExtra(EXTRA_PLAYER_RESULT) as? GameResult
            ?: GameResult.LOSS

        viewModel.level = intent.getSerializableExtra(EXTRA_DIFFICULTY) as? Difficulty
            ?: Difficulty.MEDIUM
    }

    /**
     * Настраивает пользовательский интерфейс: фон экрана, текст результата,
     * а также обработчики кнопок «Главное меню» и «Сыграть снова».
     */
    private fun setupUI() {
        binding.apply {
            var background =
                if (viewModel.result == GameResult.WIN) {
                    R.drawable.bg_victory_screen
                } else {
                    R.drawable.bg_defeat_screen
                }
            main.setBackgroundResource(background)

            tvResult.text = getString(viewModel.result.displayNameRes)

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
        val name = nickname
        viewModel.saveGameHistory(name, viewModel.result, viewModel.level)
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
        val intent = Intent(this@ResultActivity, LoadingActivity::class.java).apply {
            putParcelableArrayListExtra(
                LoadingActivity.Companion.EXTRA_PLAYER_SHIPS,
                ArrayList(viewModel.ships)
            )
            putExtra(
                LoadingActivity.Companion.EXTRA_DIFFICULTY,
                viewModel.level
            )
        }
        startActivity(intent)
        finish()
    }
}