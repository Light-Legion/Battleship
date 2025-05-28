package com.example.battleship_game.presentation.game

import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import androidx.lifecycle.lifecycleScope
import com.example.battleship_game.R
import com.example.battleship_game.ai.AiPlacementGenerator
import com.example.battleship_game.common.BaseActivity
import com.example.battleship_game.common.UserPreferences.battleDifficulty
import com.example.battleship_game.data.model.ShipPlacement
import com.example.battleship_game.databinding.ActivityLoadingBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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

    companion object {
        const val EXTRA_PLAYER_SHIPS = "EXTRA_PLAYER_SHIPS"
        const val EXTRA_COMPUTER_SHIPS = "EXTRA_COMPUTER_SHIPS"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoadingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyEdgeInsets(binding.main)
        enterImmersiveMode()

        setupUI()

        onBackPressedDispatcher.addCallback(this) {
            Snackbar.make(binding.main, R.string.hint_exit_impossible, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun setupUI() {
        // Извлекаем данные из Intent
        val playerShips = intent
            .getParcelableArrayListExtra<ShipPlacement>(EXTRA_PLAYER_SHIPS)
            .orEmpty()

        val diff = battleDifficulty

        lifecycleScope.launch {
            val aiShips = withContext(Dispatchers.Default) {
                when (diff) {

                }
            }
            // минимум 3 секунды
            delay(3_000)

            // Стартуем GameActivity
            startActivity(Intent(this@LoadingActivity, GameActivity::class.java).apply {
                putParcelableArrayListExtra(EXTRA_PLAYER_SHIPS,  ArrayList(playerShips))
                putParcelableArrayListExtra(EXTRA_COMPUTER_SHIPS, ArrayList(aiShips))
            })
            finish()
        }
    }
}