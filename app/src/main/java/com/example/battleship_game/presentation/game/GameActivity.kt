package com.example.battleship_game.presentation.game

import android.content.Intent
import android.health.connect.datatypes.units.Length
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.battleship_game.R
import com.example.battleship_game.common.BaseActivity
import com.example.battleship_game.common.UserPreferences.avatarRes
import com.example.battleship_game.common.UserPreferences.nickname
import com.example.battleship_game.data.model.Difficulty
import com.example.battleship_game.data.model.GameResult
import com.example.battleship_game.data.model.ShipPlacement
import com.example.battleship_game.databinding.ActivityGameBinding
import com.example.battleship_game.dialog.CustomAlertDialog
import com.example.battleship_game.ui.BattleFieldView
import kotlinx.coroutines.launch

class GameActivity : BaseActivity() {

    private lateinit var binding: ActivityGameBinding
    private val viewModel: GameActivityViewModel by viewModels()

    companion object {
        const val EXTRA_PLAYER_SHIPS = "EXTRA_PLAYER_SHIPS"
        const val EXTRA_COMPUTER_SHIPS = "EXTRA_COMPUTER_SHIPS"
        const val EXTRA_DIFFICULTY = "EXTRA_DIFFICULTY"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // edge-to-edge + immersive
        applyEdgeInsets(binding.main)
        enterImmersiveMode()

        parseIntentExtras()
        initBattle()
        setupUI()
    }

    /**
     * Извлекает из intent списки кораблей игрока и компьютера, а также уровень сложности.
     * Если данные отсутствуют или некорректны, ставятся значения по умолчанию.
     */
    private fun parseIntentExtras() {
        viewModel.playerShips = intent
            .getParcelableArrayListExtra<ShipPlacement>(EXTRA_PLAYER_SHIPS)
            .orEmpty()

        viewModel.computerShips = intent
            .getParcelableArrayListExtra<ShipPlacement>(EXTRA_COMPUTER_SHIPS)
            .orEmpty()

        viewModel.difficulty = (intent.getSerializableExtra(EXTRA_DIFFICULTY) as? Difficulty)
            ?: Difficulty.MEDIUM
    }

    private fun initBattle() {
        viewModel.shootingStrategy = ShootingStrategyFactory
            .createForDifficulty(viewModel.difficulty)

        viewModel.initBattle()
    }

    private fun setupUI() {
        binding.apply {
            btnGiveUp.setOnClickListener {
                showExitConfirmDialog()
            }

            // Отображаем аватар/никнейм игрока
            ivAvatarPlayer.setImageResource(avatarRes)
            tvPlayer.text = nickname

            // Устанавливаем расстановки в оба поля:
            //    слева – поле игрока, справа – поле компьютера
            bfvPlayer.fieldType = BattleFieldView.FieldType.PLAYER
            bfvPlayer.setPlacements(viewModel.playerShips)

            bfvComputer.fieldType = BattleFieldView.FieldType.COMPUTER
            bfvComputer.setPlacements(viewModel.computerShips)

            // Устанавливаем колбэк для тапов по полю компьютера:
            binding.bfvComputer.setOnCellClickListener { row, col ->
                onPlayerCellClicked(row, col)
            }

        }

        onBackPressedDispatcher.addCallback(this) {
            showExitConfirmDialog()
        }
    }

    // Пользователь кликает по полю компьютера
    private fun onPlayerCellClicked(row: Int, col: Int) {
        if (viewModel.isBattleOver || !viewModel.isPlayerTurn) {
            // бой уже кончён или сейчас ход компьютера — игнорируем
            return
        }
        // 1) Обрабатываем выстрел игрока
        val result = viewModel.playerShot(row, col)

        // 2) Если попал:
        if (result.hit) {
            // 1) Ставим крестик
            binding.bfvComputer.markHit(row, col)
            // 2) Если затопили корабль
            if (result.sunk && result.sunkShip != null) {
                binding.bfvComputer.markSunkShip(result.sunkShip, result.bufferCells)
                updateRemainingCounters(result.sunkShip.length)
            }
            // 3) Если бой закончился (победа игрока)
            if (viewModel.isBattleOver) {
                lockFields()
                navigateToResult(GameResult.WIN)
            }
            // Попал, но не все потоплены → игрок стреляет дальше
            return
        }

        // Промах
        binding.bfvComputer.markMiss(row, col)
        viewModel.isPlayerTurn = false
        switchToComputerTurn()
    }

    // Переключаем фокус на компьютер и ждём его ход
    private fun switchToComputerTurn() {
        // 1) Меняем текст «Ваш ход» → «Ход компьютера»
        binding.tvTurnMessage.setText(R.string.txt_turn_computer)
        // 2) Блокируем нажатия по полю компьютера (viewModel.isPlayerTurn уже false)
        binding.bfvComputer.isEnabled = false

        // 3) Запускаем coroutine для «хода компьютера» через 3 сек
        lifecycleScope.launch {
            val result = viewModel.computerShot()

            // 4) Отмечаем результат на поле игрока
            if (result.hit) {
                binding.bfvPlayer.markHit(result.row, result.col)
                if (result.sunk && result.sunkShip != null) {
                    binding.bfvPlayer.markSunkShip(result.sunkShip, result.bufferCells)
                }
                // Если этим ходом был потоплен последний корабль игрока:
                if (viewModel.isBattleOver) {
                    navigateToResult(GameResult.LOSS)
                    return@launch
                }
                // Попал — снова ход компьютера (повторяем всю функцию)
                switchToComputerTurn()
            } else {
                // Промах → ход возвращается игроку
                binding.bfvPlayer.markMiss(result.row, result.col)
                switchToPlayerTurn()
            }
        }
    }

    /**
     * Переход к ходу игрока: меняем надпись и разблокируем поле компьютера.
     */
    private fun switchToPlayerTurn() {
        binding.tvTurnMessage.text = getString(R.string.txt_turn_player)
        viewModel.isPlayerTurn = true
        binding.bfvComputer.isEnabled = true
    }

    /**
     * Блокирует оба поля (когда бой завершён).
     */
    private fun lockFields() {
        binding.bfvComputer.isEnabled = false
        binding.bfvPlayer.isEnabled = false
    }

    /**
     * Обновляет UI‐счётчики «сколько осталось» кораблей компьютера.
     */
    private fun updateRemainingCounters(length: Int) {
        when (length) {
            4 -> { // Battleship
                val cur = binding.tvCountBattleship.text.toString().toInt()
                binding.tvCountBattleship.text = (cur - 1).coerceAtLeast(0).toString()
            }
            3 -> { // Cruiser
                val cur = binding.tvCountCruiser.text.toString().toInt()
                binding.tvCountCruiser.text = (cur - 1).coerceAtLeast(0).toString()
            }
            2 -> { // Destroyer
                val cur = binding.tvCountDestroyer.text.toString().toInt()
                binding.tvCountDestroyer.text = (cur - 1).coerceAtLeast(0).toString()
            }
            1 -> { // SpeedBoat
                val cur = binding.tvCountSpeedBoat.text.toString().toInt()
                binding.tvCountSpeedBoat.text = (cur - 1).coerceAtLeast(0).toString()
            }
        }
    }

    // Подтверждение «Сдаться»
    private fun showExitConfirmDialog() {
        CustomAlertDialog(this)
            .setIcon(R.drawable.ic_dialog_warning)
            .setTitle(R.string.game_over_title)
            .setMessage(R.string.game_over_message)
            .setPositiveButtonText(R.string.action_yes)
            .setNegativeButtonText(R.string.action_cancel)
            .setOnPositiveClickListener {
                navigateToResult(GameResult.LOSS)
            }
            .show()
    }

    // Переход на экран результата (клáсс ResultActivity)
    private fun navigateToResult(result: GameResult) {
        val intent = Intent(this@GameActivity, ResultActivity::class.java).apply {
            putParcelableArrayListExtra(
                ResultActivity.EXTRA_PLAYER_SHIPS,
                ArrayList(viewModel.playerShips)
            )
            putExtra(
                ResultActivity.EXTRA_PLAYER_RESULT,
                result
            )
        }
        startActivity(intent)
        finish()
    }
}