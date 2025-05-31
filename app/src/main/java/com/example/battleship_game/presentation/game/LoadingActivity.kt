package com.example.battleship_game.presentation.game

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.animation.LinearInterpolator
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.core.animation.doOnEnd
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.battleship_game.R
import com.example.battleship_game.common.BaseActivity
import com.example.battleship_game.common.UserPreferences.battleDifficulty
import com.example.battleship_game.data.model.ShipPlacement
import com.example.battleship_game.databinding.ActivityLoadingBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Загрузочный экран:
 * • Показывает ProgressBar минимум 3 с
 * • Генерирует ИИ-расстановку по сложности
 * • Потом стартует GameActivity
 *
 * Ожидаемые extras:
 * - EXTRA_PLAYER_SHIPS: ArrayList<ShipPlacement> игрока
 * - EXTRA_DIFFICULTY: String ("EASY"/"MEDIUM"/"HARD")
 */
class LoadingActivity : BaseActivity() {

    private lateinit var binding: ActivityLoadingBinding
    private val viewModel: LoadingActivityViewModel by viewModels()

    private var animationComplete = false
    private var generationComplete = false

    companion object {
        const val EXTRA_PLAYER_SHIPS = "EXTRA_PLAYER_SHIPS"
        const val EXTRA_COMPUTER_SHIPS = "EXTRA_COMPUTER_SHIPS"

        private const val COUNTDOWN_DURATION_MS = 3000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoadingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyEdgeInsets(binding.main)
        enterImmersiveMode()

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        parseData()

        // Параллельно запускаем генерацию и анимацию
        startComputerShipGeneration()
        startCountdownAnimation()

        onBackPressedDispatcher.addCallback(this) {
            Snackbar.make(binding.main, R.string.hint_exit_impossible, Snackbar.LENGTH_SHORT).show()
        }
    }

    /**
     * Извлекает из intent список кораблей и уровень сложности.
     * Записывает их в viewModel.
     */
    private fun parseData() {
        // Получаем данные из интента и сохраняем в viewModel
        viewModel.playerShips = intent
            .getParcelableArrayListExtra<ShipPlacement>(EXTRA_PLAYER_SHIPS)
            .orEmpty()

        // Получаем сложность из настроек и сохраняем в viewModel
        viewModel.difficulty = battleDifficulty
    }

    /**
     * Запускает генерацию кораблей компьютера в фоновом потоке
     */
    private fun startComputerShipGeneration() {
        lifecycleScope.launch(Dispatchers.IO) {
            viewModel.computerShips = viewModel.generateComputerShips(viewModel.difficulty)
            generationComplete = true

            // Если анимация уже завершена, сразу переходим
            if (animationComplete) {
                withContext(Dispatchers.Main) {
                    startGameActivity()
                }
            }
        }
    }

    /**
     * Запускает анимацию таймера и прогресс-бара
     */
    private fun startCountdownAnimation() {
        // Анимация прогресс-бара (от 100 до 0 за 3 секунды)
        val progressAnimator = ObjectAnimator.ofInt(binding.progressBar, "progress", 100, 0).apply {
            duration = COUNTDOWN_DURATION_MS
            interpolator = LinearInterpolator()
        }

        // Анимация текста таймера (от 3 до 0)
        val timerAnimator = ValueAnimator.ofInt(3, 0).apply {
            duration = COUNTDOWN_DURATION_MS
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                val currentValue = animation.animatedValue as Int
                binding.tvTimer.text = currentValue.toString()
            }
        }

        // Запускаем анимации вместе
        progressAnimator.start()
        timerAnimator.start()

        // Обработка завершения анимации
        progressAnimator.doOnEnd {
            animationComplete = true

            // Если генерация завершена, сразу переходим
            if (generationComplete) {
                startGameActivity()
            } else {
                // Иначе ждем завершения генерации
                lifecycleScope.launch(Dispatchers.IO) {
                    // Дожидаемся завершения генерации
                    while (!generationComplete) {
                        Thread.sleep(50)
                    }

                    withContext(Dispatchers.Main) {
                        startGameActivity()
                    }
                }
            }
        }
    }

    /**
     * Переход на экран игры
     */
    private fun startGameActivity() {
        val playerShips = intent.getParcelableArrayListExtra<ShipPlacement>(EXTRA_PLAYER_SHIPS) ?: arrayListOf()

        startActivity(Intent(this@LoadingActivity, GameActivity::class.java).apply {
            putParcelableArrayListExtra(EXTRA_PLAYER_SHIPS, ArrayList(viewModel.playerShips))
            putParcelableArrayListExtra(EXTRA_COMPUTER_SHIPS, ArrayList(viewModel.computerShips))
        })
        finish()
    }
}