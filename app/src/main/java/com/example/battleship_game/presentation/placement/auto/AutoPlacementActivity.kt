package com.example.battleship_game.presentation.placement.auto

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.StringRes
import com.example.battleship_game.R
import com.example.battleship_game.common.BaseActivity
import com.example.battleship_game.data.model.Difficulty
import com.example.battleship_game.databinding.ActivityAutoPlacementBinding
import com.example.battleship_game.dialog.CustomAlertDialog
import com.example.battleship_game.presentation.game.LoadingActivity
import com.example.battleship_game.presentation.placement.PlacementStrategyType
import com.example.battleship_game.presentation.placement.save.SavePlacementActivity
import com.google.android.material.snackbar.Snackbar

/**
 * Экран «Автоматическая расстановка».
 * Пользователь выбирает одну из трёх стратегий, обновляет поле,
 * может сохранить расстановку или сразу перейти в бой.
 */
class AutoPlacementActivity : BaseActivity() {

    private lateinit var binding: ActivityAutoPlacementBinding
    private val viewModel: AutoPlacementViewModel by viewModels()

    companion object {
        const val EXTRA_DIFFICULTY = "EXTRA_DIFFICULTY"
    }

    private val savePlacementLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // Показываем сообщение, только если расстановка была сохранена
            Snackbar.make(binding.main, R.string.hint_save_placement, Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAutoPlacementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyEdgeInsets(binding.main)
        enterImmersiveMode()

        parseIntentExtras()
        setupDropdown()
        setupListeners()
        observeViewModel()

        onBackPressedDispatcher.addCallback(this) {
            finish()
        }
    }

    private fun parseIntentExtras() {
        // Получаем сложность из настроек и сохраняем в viewModel
        viewModel.difficulty = intent
            .getSerializableExtra(EXTRA_DIFFICULTY) as? Difficulty ?: Difficulty.MEDIUM
    }

    /** Заполняем выпадающий список стратегий. */
    private fun setupDropdown() {
        val strategies = resources.getStringArray(R.array.strategies_placement)

        binding.actvStrategy.setAdapter(
            ArrayAdapter(this, R.layout.list_item_dropdown, strategies)
        )
    }

    /** Подключаем логику кнопок. */
    private fun setupListeners() {
        binding.apply {
            // Кнопка "Назад"
            btnBack.setOnClickListener {
                finish()
            }

            // Кнопка "Обновить расстановку"
            btnUpdate.setOnClickListener {
                val sel = actvStrategy.text.toString()
                if (sel.isBlank()) {
                    showExitConfirmDialog(R.string.error_refresh_title, R.string.error_refresh_message)
                    return@setOnClickListener
                }

                // Генерируем новую расстановку
                val type = PlacementStrategyType.fromString(this@AutoPlacementActivity, sel)
                viewModel.generate(type)

                val ships = viewModel.placement.value
                if (ships.isNullOrEmpty()) {
                    Snackbar.make(binding.root, R.string.hint_error_placement, Snackbar.LENGTH_LONG).show()
                } else {
                    bfv.setPlacements(ships)
                }
            }

            // Кнопка "Сохранить расстановку"
            btnSave.setOnClickListener {
                val ships = viewModel.placement.value ?: emptyList()

                if (ships.isEmpty()) {
                    showExitConfirmDialog(R.string.error_save_title, R.string.error_save_message)
                    return@setOnClickListener
                }

                savePlacementLauncher.launch(Intent(this@AutoPlacementActivity, SavePlacementActivity::class.java)
                    .putParcelableArrayListExtra(
                        SavePlacementActivity.EXTRA_SHIPS,
                        ArrayList(ships)
                    )
                )
            }

            // Кнопка "В бой!"
            btnToBattle.setOnClickListener {
                val ships = viewModel.placement.value!!

                startActivity(
                    Intent(this@AutoPlacementActivity, LoadingActivity::class.java).apply {
                        putParcelableArrayListExtra(
                            LoadingActivity.EXTRA_PLAYER_SHIPS,
                            ArrayList(ships)
                        )
                        putExtra(
                            EXTRA_DIFFICULTY,
                            viewModel.difficulty
                        )
                    })
                finish()
            }
        }
    }

    /** Подписываемся на изменения ViewModel, чтобы обновлять доступность кнопок. */
    private fun observeViewModel() {
        viewModel.hasPlacementLive.observe(this) { has ->
            binding.btnToBattle.isEnabled = has
        }
    }

    private fun showExitConfirmDialog(@StringRes title : Int, @StringRes message : Int) {
        CustomAlertDialog(this)
            .setIcon(R.drawable.ic_dialog_warning)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButtonText(R.string.action_ok)
            .show()
    }
}