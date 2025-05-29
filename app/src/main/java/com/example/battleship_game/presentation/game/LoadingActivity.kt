package com.example.battleship_game.presentation.game

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.animation.LinearInterpolator
import androidx.activity.addCallback
import androidx.core.animation.doOnEnd
import androidx.lifecycle.lifecycleScope
import com.example.battleship_game.R
import com.example.battleship_game.common.BaseActivity
import com.example.battleship_game.common.UserPreferences.battleDifficulty
import com.example.battleship_game.data.model.ShipPlacement
import com.example.battleship_game.databinding.ActivityLoadingBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Загрузочный экран:
 * • Показывает круговой ProgressBar с обратным отсчётом 3 с
 * • Генерирует ИИ-расстановку по сложности
<<<<<<< Updated upstream
 * • Стартует GameActivity
=======
 * • Потом стартует GameActivity
 *
 * Ожидаемые extras:
 * - EXTRA_PLAYER_SHIPS: ArrayList<ShipPlacement> игрока
>>>>>>> Stashed changes
 */
class LoadingActivity : BaseActivity() {

    private lateinit var binding: ActivityLoadingBinding

    companion object {
        const val EXTRA_PLAYER_SHIPS = "EXTRA_PLAYER_SHIPS"
        const val EXTRA_COMPUTER_SHIPS = "EXTRA_COMPUTER_SHIPS"
        private const val COUNTDOWN_MS = 3000
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoadingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyEdgeInsets(binding.main)
        enterImmersiveMode()

        setupUI()
        startCountdown()

        onBackPressedDispatcher.addCallback(this) {
            Snackbar.make(binding.main, R.string.hint_exit_impossible, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun setupUI() {
        // Считываем корабли игрока
        // Отключено: генерация и переход будет в анимации
    }

    private fun startCountdown() {
        // Инициализируем progressBar
        binding.progressBar.max = COUNTDOWN_MS
        binding.progressBar.progress = COUNTDOWN_MS

        // Анимация прогресса от max до 0 за COUNTDOWN_MS
        ObjectAnimator.ofInt(binding.progressBar, "progress", COUNTDOWN_MS, 0).apply {
            duration = COUNTDOWN_MS.toLong()
            interpolator = LinearInterpolator()
            addUpdateListener { anim ->
                val current = anim.animatedValue as Int
                val secondsLeft = (current / 1000) + 1
                binding.countdownText.text = if (current > 0) secondsLeft.toString() else ""
            }
            doOnEnd {
                // После завершения отсчёта
                binding.countdownText.text = ""
                binding.progressBar.progress = 0

                // Запускаем генерацию ИИ и переход
                lifecycleScope.launch {
                    val playerShips =
                        intent.getParcelableArrayListExtra<ShipPlacement>(EXTRA_PLAYER_SHIPS)
                            .orEmpty()
                    val diff = battleDifficulty

                    val aiShips = withContext(Dispatchers.Default) {
                        // TODO: заменить на реальный вызов генератора
                        emptyList<ShipPlacement>()
                    }

                    // Переход в GameActivity
                    startActivity(Intent(this@LoadingActivity, GameActivity::class.java).apply {
                        putParcelableArrayListExtra(EXTRA_PLAYER_SHIPS, ArrayList(playerShips))
                        putParcelableArrayListExtra(EXTRA_COMPUTER_SHIPS, ArrayList(aiShips))
                    })
                    finish()
                }
            }
            start()

            val playerShips = intent
                .getParcelableArrayListExtra<ShipPlacement>(EXTRA_PLAYER_SHIPS)
                .orEmpty()

            val difficulty = battleDifficulty

            lifecycleScope.launch {
                try {
                    // Запускаем две задачи параллельно: генерацию кораблей и задержку
                    val (aiShips) = awaitAll(
                        async(Dispatchers.Default) {
                            // generateAiShips(difficulty)
                        },
                        async { delay(3000) } // Минимальная задержка 3 секунды
                    )

                    // Переходим в GameActivity
                    startActivity(Intent(this@LoadingActivity, GameActivity::class.java).apply {
                        putParcelableArrayListExtra(EXTRA_PLAYER_SHIPS, ArrayList(playerShips))
                        //putParcelableArrayListExtra(EXTRA_COMPUTER_SHIPS, ArrayList(aiShips))
                    })
                    finish()
                } catch (e: Exception) {
                    // Обработка ошибок генерации
                    Snackbar.make(binding.main, R.string.error_name_title, Snackbar.LENGTH_LONG)
                        .show()
                    delay(2000)
                    finish()
                }
            }
        }

        suspend fun generateAiShips(difficulty: String): List<ShipPlacement> {
            return withContext(Dispatchers.Default) {
                // Ваша логика генерации кораблей ИИ
                emptyList()
            }
        }
    }
}
