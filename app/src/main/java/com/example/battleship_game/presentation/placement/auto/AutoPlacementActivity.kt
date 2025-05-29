package com.example.battleship_game.presentation.placement.auto

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.activity.addCallback
import androidx.activity.viewModels
import com.example.battleship_game.R
import com.example.battleship_game.common.BaseActivity
import com.example.battleship_game.databinding.ActivityAutoPlacementBinding
import com.example.battleship_game.dialog.CustomAlertDialog
import com.example.battleship_game.presentation.game.LoadingActivity
import com.example.battleship_game.presentation.placement.auto.AutoPlacementViewModel
import com.example.battleship_game.presentation.placement.save.SavePlacementActivity
import com.google.android.material.snackbar.Snackbar

/**
 * Экран «Автоматическая расстановка».
 * Пользователь выбирает одну из трёх стратегий, обновляет поле,
 * может сохранить расстановку или сразу перейти в бой.
 */
class AutoPlacementActivity : BaseActivity() {

    private lateinit var binding: ActivityAutoPlacementBinding
    private val vm: AutoPlacementViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAutoPlacementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyEdgeInsets(binding.main)
        enterImmersiveMode()

        setupDropdown()
        setupListeners()
        observeViewModel()

        onBackPressedDispatcher.addCallback(this) {
            finish()
        }
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
                    showExitConfirmDialog()
                    return@setOnClickListener
                }
                val placement = vm.generatePlacement(this@AutoPlacementActivity, sel)
                if (placement.isNotEmpty()) {
                    bfv.setPlacements(placement)
                } else {
                    Snackbar.make(root, R.string.hint_error_placement, Snackbar.LENGTH_LONG).show()
                }
            }

            // Кнопка "Сохранить расстановку"
            btnSave.setOnClickListener {
                vm.currentPlacement.takeIf { it.isNotEmpty() }?.let { ships ->
                    startActivity(
                        Intent(this@AutoPlacementActivity, SavePlacementActivity::class.java).apply {
                            putParcelableArrayListExtra(
                                SavePlacementActivity.EXTRA_SHIPS,
                                ArrayList(ships)
                            )
                        }
                    )
                }
            }

            // Кнопка "В бой!"
            btnToBattle.setOnClickListener {
                vm.currentPlacement.takeIf { it.isNotEmpty() }?.let { ships ->
                    startActivity(
                        Intent(this@AutoPlacementActivity, LoadingActivity::class.java).apply {
                            putParcelableArrayListExtra(
                                LoadingActivity.EXTRA_PLAYER_SHIPS,
                                ArrayList(ships)
                            )
                        }
                    )
                    finish()
                }
            }
        }
    }

    /** Подписываемся на изменения ViewModel, чтобы обновлять доступность кнопок. */
    private fun observeViewModel() {
        vm.hasPlacementLive.observe(this) { has ->
            binding.btnSave.isEnabled     = has
            binding.btnToBattle.isEnabled = has
        }
    }

    private fun showExitConfirmDialog() {
        CustomAlertDialog(this)
            .setIcon(R.drawable.ic_launcher_foreground)
            .setTitle(R.string.error_refresh_title)
            .setMessage(R.string.error_refresh_message)
            .setPositiveButtonText(R.string.action_ok)
            .show()
    }
}