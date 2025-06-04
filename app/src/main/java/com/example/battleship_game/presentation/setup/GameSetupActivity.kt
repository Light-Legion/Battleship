package com.example.battleship_game.presentation.setup

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.activity.addCallback
import com.example.battleship_game.R
import com.example.battleship_game.common.BaseActivity
import com.example.battleship_game.data.model.Difficulty
import com.example.battleship_game.databinding.ActivityGameSetupBinding
import com.example.battleship_game.presentation.placement.load.LoadSavedPlacementActivity
import com.example.battleship_game.presentation.placement.manual.ManualPlacementActivity
import com.example.battleship_game.presentation.placement.auto.AutoPlacementActivity
import com.google.android.material.snackbar.Snackbar


/**
 * Экран выбора параметров боя:
 *  - уровень сложности ИИ
 *  - способ расстановки кораблей
 *
 * Сохраняет выбор в SharedPreferences и
 * при нажатии "Далее" переходит на соответствующий экран.
 */
class GameSetupActivity : BaseActivity() {

    private lateinit var binding: ActivityGameSetupBinding

    companion object {
        const val EXTRA_DIFFICULTY = "EXTRA_DIFFICULTY"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityGameSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // edge-to-edge + immersive
        applyEdgeInsets(binding.main)
        enterImmersiveMode()

        setupDropdowns()
        setupListeners()

        onBackPressedDispatcher.addCallback(this) {
            finish()
        }
    }

    /** Инициализируем оба выпадающих списка и задаём им дефолтные значения. */
    private fun setupDropdowns() {

        // 1) Сложность (через enum)
        val difficultyDisplayNames = Difficulty.getDisplayNames(this)
        ArrayAdapter(this, R.layout.list_item_dropdown, difficultyDisplayNames).also { adapter ->
            binding.actvDifficulty.setAdapter(adapter)
            binding.actvDifficulty.setText(difficultyDisplayNames[1], false) // По умолчанию: Средний
        }

        // 2) Способ расстановки (PlacementType уже реализован)
        val placementDisplayNames = PlacementType.getDisplayNames(this)
        ArrayAdapter(this, R.layout.list_item_dropdown, placementDisplayNames).also { adapter ->
            binding.actvPlacement.setAdapter(adapter)
            binding.actvPlacement.setText(placementDisplayNames[0], false) // По умолчанию: Ручной
        }
    }

    /** Кнопки "Назад" и "Далее". */
    private fun setupListeners() {
        binding.apply {
            btnBack.setOnClickListener {
                finish()
            }

            btnNext.setOnClickListener {
                // 1) Читаем user choice
                val chosenDifficulty = binding.actvDifficulty.text.toString()
                val chosenPlacement = binding.actvPlacement.text.toString()

                // 2) Сохраняем только сложность
                val battleDifficulty = Difficulty.fromDisplayName(this@GameSetupActivity, chosenDifficulty)

                // 3) По индексу способа выбираем Activity
                val placementType = PlacementType.fromDisplayName(this@GameSetupActivity, chosenPlacement)

                val activityClass = when (placementType) {
                    PlacementType.MANUAL     -> ManualPlacementActivity::class.java
                    PlacementType.AUTO       -> AutoPlacementActivity::class.java
                    PlacementType.LOAD_SAVED -> LoadSavedPlacementActivity::class.java
                    else -> null
                }

                if (activityClass != null) {
                    startActivity(Intent(this@GameSetupActivity, activityClass).apply {
                        putExtra(
                            EXTRA_DIFFICULTY,
                            battleDifficulty
                        )
                    })
                } else {
                    Snackbar.make(main, R.string.hint_select_setup, Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }
}