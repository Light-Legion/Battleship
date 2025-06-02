package com.example.battleship_game.presentation.game

import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.viewModels
import com.example.battleship_game.R
import com.example.battleship_game.common.BaseActivity
import com.example.battleship_game.common.UserPreferences.avatarRes
import com.example.battleship_game.common.UserPreferences.nickname
import com.example.battleship_game.data.model.GameResult
import com.example.battleship_game.data.model.ShipPlacement
import com.example.battleship_game.databinding.ActivityGameBinding
import com.example.battleship_game.dialog.CustomAlertDialog

class GameActivity : BaseActivity() {

    private lateinit var binding: ActivityGameBinding
    private val viewModel: GameActivityViewModel by viewModels()

    companion object {
        const val EXTRA_PLAYER_SHIPS = "EXTRA_PLAYER_SHIPS"
        const val EXTRA_COMPUTER_SHIPS = "EXTRA_COMPUTER_SHIPS"
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
        //setupGame()
    }

    /**
     * Извлекает из intent список кораблей и результат матча.
     * Если данные отсутствуют или некорректны, ставятся значения по умолчанию.
     */
    private fun parseIntentExtras() {
        viewModel.playerShips = intent
            .getParcelableArrayListExtra<ShipPlacement>(EXTRA_PLAYER_SHIPS)
            .orEmpty()

        viewModel.computerShips = intent
            .getParcelableArrayListExtra<ShipPlacement>(EXTRA_COMPUTER_SHIPS)
            .orEmpty()
    }

    private fun setupUI() {
        binding.apply {
            btnGiveUp.setOnClickListener {
                showExitConfirmDialog()
            }

            bfvPlayer.setPlacements(viewModel.playerShips)
            bfvComputer.setPlacements(viewModel.computerShips)

            ivAvatarPlayer.setImageResource(avatarRes)
            tvPlayer.text = nickname
        }

        onBackPressedDispatcher.addCallback(this) {
            showExitConfirmDialog()
        }
    }

    private fun openResult(result: GameResult) {
        startActivity(
            Intent(this@GameActivity, ResultActivity::class.java)
                .putParcelableArrayListExtra(
                    ResultActivity.EXTRA_PLAYER_SHIPS,
                    ArrayList(viewModel.playerShips)
                )
                .putExtra(
                    ResultActivity.EXTRA_PLAYER_RESULT,
                    result
                )
        )
        finish()
    }

    private fun showExitConfirmDialog() {
        CustomAlertDialog(this)
            .setIcon(R.drawable.ic_dialog_warning)
            .setTitle(R.string.game_over_title)
            .setMessage(R.string.game_over_message)
            .setPositiveButtonText(R.string.action_yes)
            .setNegativeButtonText(R.string.action_cancel)
            .setOnPositiveClickListener {
                openResult(GameResult.LOSS)
            }
            .show()
    }
}