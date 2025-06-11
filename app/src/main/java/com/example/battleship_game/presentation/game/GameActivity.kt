package com.example.battleship_game.presentation.game

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.battleship_game.R
import com.example.battleship_game.audio.SoundEffectsManager
import com.example.battleship_game.common.BaseActivity
import com.example.battleship_game.common.UserPreferences.avatarRes
import com.example.battleship_game.common.UserPreferences.nickname
import com.example.battleship_game.data.model.Difficulty
import com.example.battleship_game.data.model.GameResult
import com.example.battleship_game.data.model.ShipPlacement
import com.example.battleship_game.databinding.ActivityGameBinding
import com.example.battleship_game.dialog.CustomAlertDialog
import com.example.battleship_game.presentation.result.ResultActivity
import com.example.battleship_game.strategies.shooting.AdaptiveDensityStrategy
import com.example.battleship_game.ui.BattleFieldView
import kotlinx.coroutines.launch

class GameActivity : BaseActivity() {

    private lateinit var binding: ActivityGameBinding
    private val viewModel: GameActivityViewModel by viewModels()

    // Менеджер звуковых эффектов
    private lateinit var soundEffectsManager: SoundEffectsManager

    companion object {
        const val EXTRA_PLAYER_SHIPS = "EXTRA_PLAYER_SHIPS"
        const val EXTRA_COMPUTER_SHIPS = "EXTRA_COMPUTER_SHIPS"
        const val EXTRA_DIFFICULTY = "EXTRA_DIFFICULTY"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Инициализируем менеджер звуковых эффектов
        soundEffectsManager = SoundEffectsManager(applicationContext)

        // edge-to-edge + immersive
        applyEdgeInsets(binding.main)
        enterImmersiveMode()

        parseIntentExtras()
        initBattle()
        setupUI()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Освобождаем ресурсы SoundPool
        soundEffectsManager.release()
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

        // 3) Если стратегия — именно DensityAnalysisStrategy, даём ей “provider”:
        if (viewModel.shootingStrategy is AdaptiveDensityStrategy) {
            (viewModel.shootingStrategy as AdaptiveDensityStrategy).setEnemyShipProvider {
                // Лямбда, которая вернет ВСЕ живые палубы игрока в текущий момент:
                viewModel.getAllLivePlayerDecks()
            }
        }
    }

    private fun setupUI() {
        binding.apply {
            tvTurnMessage.viewTreeObserver.addOnGlobalLayoutListener(
                object : ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        binding.tvTurnMessage.viewTreeObserver.removeOnGlobalLayoutListener(this)
                        startPulseAnimation(isPlayer = true)
                    }
                }
            )

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
        // Если бой уже закончился или это не ход игрока — игнорируем
        if (viewModel.isBattleOver || !viewModel.isPlayerTurn) return

        // 1) Обрабатываем выстрел игрока
        val result = viewModel.playerShot(row, col)

        // 2) Если попал:
        if (result.hit) {
            // 2.1) Сначала нарисовать крестик:
            binding.bfvComputer.markHit(row, col)

            // 2.2) Если этим ходом корабль потоплен:
            if (result.sunk && result.sunkShip != null) {
                // Последняя палуба → затопление: звук затопления
                soundEffectsManager.playSunkSound()

                binding.bfvComputer.markSunkShip(result.sunkShip, result.bufferCells)
                updateRemainingCounters(result.sunkShip.length)
            } else {
                // Просто попадание в палубу (не полное затопление)
                soundEffectsManager.playHitSound()
            }

            // 2.3) Проверяем: не закончилась ли битва?
            if (viewModel.isBattleOver) {
                lockFields()
                navigateToResult(GameResult.WIN)
                return
            }
            // 2.4) Иначе — игрок стреляет ещё раз (ход не меняется)
            return
        }

        // Промах → рисуем штриховку и звук промаха
        binding.bfvComputer.markMiss(row, col)
        soundEffectsManager.playMissSound()

        viewModel.isPlayerTurn = false
        switchToComputerTurn(startedFromMiss = false)
    }

    // Переключаем фокус на компьютер и ждём его ход
    private fun switchToComputerTurn(startedFromMiss: Boolean) {
        if (!startedFromMiss) {
            // (Можно повторно анимировать “ход компьютера”)
            startPulseAnimation(isPlayer = false)
            binding.tvTurnMessage.setText(R.string.txt_turn_computer)
            binding.bfvComputer.isEnabled = false
        }

        // 3) Запускаем coroutine для «хода компьютера» через 3 сек
        lifecycleScope.launch {
            val result = viewModel.computerShot()

            if (result.hit) {
                // 1) Ставим крестик
                binding.bfvPlayer.markHit(result.row, result.col)

                // 2) Если затопил корабль:
                if (result.sunk && result.sunkShip != null) {
                    // Затопление корабля игрока
                    soundEffectsManager.playSunkSound()

                    binding.bfvPlayer.markSunkShip(result.sunkShip, result.bufferCells)
                } else {
                    // Просто попадание
                    soundEffectsManager.playHitSound()
                }

                // 3) Если сейчас бой закончен — сразу уходим
                if (viewModel.isBattleOver) {
                    lockFields()
                    navigateToResult(GameResult.LOSS)
                    return@launch
                }

                // 4) Компьютер попал, значит он стреляет ещё раз:
                switchToComputerTurn(startedFromMiss = true)
            } else {
                // 5) Компьютер промахнулся: → рисуем штриховку и звук промаха
                binding.bfvPlayer.markMiss(result.row, result.col)
                soundEffectsManager.playMissSound()

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
        startPulseAnimation(isPlayer = true)
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

    private fun startPulseAnimation(isPlayer : Boolean) {
        binding.tvTurnMessage.clearAnimation()

        setTurnIndicatorStyle(
            if (isPlayer) R.color.player_turn_background
            else R.color.computer_turn_background,
            if (isPlayer) R.color.player_turn_text
            else R.color.computer_turn_text
        )

        val animator = ObjectAnimator.ofPropertyValuesHolder(
            binding.tvTurnMessage,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 0.9f, 1.1f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.9f, 1.1f)
        ).apply {
            duration = 300
            repeatCount = 4 // 3 пульсации = 6 полуциклов, но 5 повторений даст 3 полных
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()

            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    binding.tvTurnMessage.scaleX = 1.0f
                    binding.tvTurnMessage.scaleY = 1.0f
                }
            })
        }

        animator.start()
    }

    private fun setTurnIndicatorStyle(
        @ColorRes backgroundColor: Int,
        @ColorRes textColor: Int
    ) {
        val context = binding.tvTurnMessage.context
        val bgColor = ContextCompat.getColor(context, backgroundColor)
        val txtColor = ContextCompat.getColor(context, textColor)

        // Обновляем фон
        val drawable = binding.tvTurnMessage.background.mutate()
        if (drawable is GradientDrawable) {
            drawable.setColor(bgColor)
            binding.tvTurnMessage.background = drawable
        }

        // Обновляем цвет текста
        binding.tvTurnMessage.setTextColor(txtColor)
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
            putExtra(
                ResultActivity.EXTRA_DIFFICULTY,
                viewModel.difficulty
            )

        }
        startActivity(intent)
        finish()
    }
}