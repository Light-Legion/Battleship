package com.example.battleship_game.presentation.placement.load

import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.battleship_game.R
import com.example.battleship_game.common.BaseActivity
import com.example.battleship_game.data.model.Difficulty
import com.example.battleship_game.databinding.ActivityLoadSavedPlacementBinding
import com.example.battleship_game.presentation.placement.manual.ManualPlacementActivity
import com.google.android.material.snackbar.Snackbar

/**
 * Экран загрузки сохранённой расстановки.
 * Пользователь выбирает одну из сохранённых и нажимает «Загрузить».
 */
class LoadSavedPlacementActivity : BaseActivity() {

    private lateinit var binding: ActivityLoadSavedPlacementBinding
    private val viewModel: LoadSavedPlacementViewModel by viewModels()
    private var selectedId: Long? = null

    companion object {
        const val EXTRA_DIFFICULTY = "EXTRA_DIFFICULTY"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoadSavedPlacementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyEdgeInsets(binding.main)
        enterImmersiveMode()

        parseIntentExtras()
        setupUI()

        // Перехват системной кнопки «Назад»
        onBackPressedDispatcher.addCallback(this) {
            finish()
        }
    }

    private fun parseIntentExtras() {
        // Получаем сложность из настроек и сохраняем в viewModel
        viewModel.difficulty = intent
            .getSerializableExtra(EXTRA_DIFFICULTY) as? Difficulty ?: Difficulty.MEDIUM
    }

    /**
     * Инициализирует UI:
     * 1. RecyclerView + адаптер + подсветка выбранного элемента
     * 2. Сбор данных из ViewModel через Flow
     * 3. Обработчик нажатия «Загрузить»
     */
    private fun setupUI() {
        binding.apply {
            btnBack.setOnClickListener {
                finish()
            }

            // 1) Настроим RecyclerView и адаптер
            val adapter = SavedPlacementAdapter(emptyList()) {
                selectedId = it.placementId
            }
            rvSavedFields.apply {
                layoutManager = LinearLayoutManager(this@LoadSavedPlacementActivity)
                this.adapter = adapter
            }

            // 2) Сбор списка из ViewModel
            lifecycleScope.launchWhenStarted {
                viewModel.placements.collect { list ->
                    adapter.submitList(list)
                }
            }

            // 3) Обработчик кнопки «Загрузить»
            btnLoad.setOnClickListener {
                selectedId?.let { id ->
                    // Передаём ID в ManualPlacementActivity
                    startActivity(
                        Intent(this@LoadSavedPlacementActivity, ManualPlacementActivity::class.java).apply {
                            putExtra(ManualPlacementActivity.EXTRA_FIELD_ID, id)
                            putExtra(EXTRA_DIFFICULTY, viewModel.difficulty)
                        })
                } ?: run {
                    // Если ничего не выбрано — предупредим пользователя
                    Snackbar.make(main, R.string.hint_select_setup, Snackbar.LENGTH_SHORT).show()
                }

            }
        }
    }
}