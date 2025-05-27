package com.example.battleship_game.presentation.setup

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.activity.addCallback
import com.example.battleship_game.R
import com.example.battleship_game.common.BaseActivity
import com.example.battleship_game.common.UserPreferences.battleDifficulty
import com.example.battleship_game.databinding.ActivityGameSetupBinding
import com.example.battleship_game.presentation.placement.LoadSavedPlacementActivity
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

    private lateinit var difficulties: Array<String>
    private lateinit var placements:   Array<String>

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
        difficulties = resources.getStringArray(R.array.difficulty_options)
        placements   = resources.getStringArray(R.array.placement_options)

        // 1) Сложность
        ArrayAdapter(this, R.layout.list_item_dropdown, difficulties).also { adapter ->
            binding.actvDifficulty.setAdapter(adapter)
            binding.actvDifficulty.setText(difficulties[1], false)  // по умолчанию "Средний"
        }
        // 2) Способ расстановки
        ArrayAdapter(this, R.layout.list_item_dropdown, placements).also { adapter ->
            binding.actvPlacement.setAdapter(adapter)
            binding.actvPlacement.setText(placements[0], false)    // по умолчанию "Ручной"
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
                val chosenPlacement  = binding.actvPlacement.text.toString()

                // 2) Сохраняем только сложность
                battleDifficulty = chosenDifficulty

                // 3) По индексу способа выбираем Activity
                val index = placements.indexOf(chosenPlacement)
                /*val nextCls = when (index) {
                    0 -> { ManualPlacementActivity::class.java
                    1 -> AutoPlacementActivity::class.java
                    2 -> LoadSavedFieldActivity::class.java
                    else -> ManualPlacementActivity::class.java
                }

                startActivity(Intent(this@GameSetupActivity, nextCls))*/

                when (index) {
                    0 -> Snackbar.make(main, "Переход в ручную расстановку", Snackbar.LENGTH_SHORT).show();
                    1 -> Snackbar.make(main, "Переход в автоматическую расстановку", Snackbar.LENGTH_SHORT).show()
                    2 -> {
                        Snackbar.make(
                            main,
                            "Переход в загрузку сохраненной расстановки",
                            Snackbar.LENGTH_SHORT
                        ).show()
                        startActivity(Intent(this@GameSetupActivity, LoadSavedPlacementActivity::class.java))
                    }
                    else -> Snackbar.make(main, "Переход по кнопке справка", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }
}