package com.example.battleship_game.presentation.game

import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import com.example.battleship_game.R
import com.example.battleship_game.common.BaseActivity
import com.example.battleship_game.data.model.GameResult
import com.example.battleship_game.data.model.ShipPlacement
import com.example.battleship_game.databinding.ActivityResultBinding
import com.example.battleship_game.presentation.main.MainActivity

class ResultActivity : BaseActivity() {

    private lateinit var binding: ActivityResultBinding
    // Список расстановки кораблей игрока
    private lateinit var ships: List<ShipPlacement>
    // Результат матча (WIN или LOSS)
    private lateinit var result: GameResult

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

        parseIntentExtras()
        setupUI()

        onBackPressedDispatcher.addCallback(this) {
            navigateToMain()
        }
    }

    /**
     * Извлекает из intent список кораблей и результат матча.
     * Если данные отсутствуют или некорректны, ставятся значения по умолчанию.
     */
    private fun parseIntentExtras() {
        // 1) Получаем список ShipPlacement (через ParcelableArrayList)
        ships = intent
            .getParcelableArrayListExtra<ShipPlacement>(EXTRA_PLAYER_SHIPS)
            .orEmpty()

        // 2) Получаем GameResult (enum) через Serializable
        val serialized = intent.getSerializableExtra(EXTRA_PLAYER_RESULT)
        result = serialized as? GameResult ?: GameResult.LOSS
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