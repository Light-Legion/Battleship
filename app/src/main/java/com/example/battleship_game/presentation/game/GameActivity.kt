package com.example.battleship_game.presentation.game

import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import com.example.battleship_game.R
import com.example.battleship_game.common.BaseActivity
import com.example.battleship_game.data.model.GameResult
import com.example.battleship_game.data.model.ShipPlacement
import com.example.battleship_game.databinding.ActivityGameBinding
import com.example.battleship_game.dialog.CustomAlertDialog

class GameActivity : BaseActivity() {

    private lateinit var binding: ActivityGameBinding

    private lateinit var playerShips: List<ShipPlacement>
    private lateinit var computerShips: List<ShipPlacement>

    companion object {
        const val EXTRA_PLAYER_SHIPS = "EXTRA_PLAYER_SHIPS"
        const val EXTRA_PLAYER_RESULT = "EXTRA_PLAYER_RESULT"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // edge-to-edge + immersive
        applyEdgeInsets(binding.main)
        enterImmersiveMode()

        parseIntentExtras()
        setupUI()

        onBackPressedDispatcher.addCallback(this) {
            showExitConfirmDialog()
        }
    }

    /**
     * Извлекает из intent список кораблей и результат матча.
     * Если данные отсутствуют или некорректны, ставятся значения по умолчанию.
     */
    private fun parseIntentExtras() {
        playerShips = intent
            .getParcelableArrayListExtra<ShipPlacement>(ResultActivity.Companion.EXTRA_PLAYER_SHIPS)
            .orEmpty()

        computerShips = intent
            .getParcelableArrayListExtra<ShipPlacement>(ResultActivity.Companion.EXTRA_PLAYER_SHIPS)
            .orEmpty()
    }

    private fun setupUI() {
        binding.apply {
            btnWin.setOnClickListener {
                startActivity(
                    Intent(this@GameActivity, ResultActivity::class.java)
                        .putParcelableArrayListExtra(
                            ResultActivity.EXTRA_PLAYER_SHIPS,
                            ArrayList(playerShips)
                        )
                        .putExtra(
                            ResultActivity.EXTRA_PLAYER_RESULT,
                            GameResult.WIN
                        )
                )
                finish()
            }

            btnLoss.setOnClickListener {
                startActivity(
                    Intent(this@GameActivity, ResultActivity::class.java)
                        .putParcelableArrayListExtra(
                            ResultActivity.EXTRA_PLAYER_SHIPS,
                            ArrayList(playerShips)
                        )
                        .putExtra(
                            ResultActivity.EXTRA_PLAYER_RESULT,
                            GameResult.LOSS
                        )
                )
                finish()
            }
        }
    }

    private fun showExitConfirmDialog() {
        CustomAlertDialog(this)
            .setIcon(R.drawable.ic_dialog_warning)
            .setTitle(R.string.game_over_title)
            .setMessage(R.string.game_over_message)
            .setPositiveButtonText(R.string.action_yes)
            .setNegativeButtonText(R.string.action_cancel)
            .setOnPositiveClickListener {
                startActivity(
                    Intent(this@GameActivity, ResultActivity::class.java)
                        .putParcelableArrayListExtra(
                            ResultActivity.EXTRA_PLAYER_SHIPS,
                            ArrayList(playerShips)
                        )
                        .putExtra(
                            ResultActivity.EXTRA_PLAYER_RESULT,
                            GameResult.LOSS
                        )
                )
                finish()
            }
            .show()
    }
}