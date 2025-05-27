package com.example.battleship_game.presentation.placement

import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.battleship_game.R
import com.example.battleship_game.common.BaseActivity
import com.example.battleship_game.databinding.ActivityLoadSavedFieldBinding
import com.google.android.material.snackbar.Snackbar


/**
 * Экран загрузки сохранённой расстановки.
 * Пользователь выбирает одну из сохранённых и нажимает «Загрузить».
 */
class LoadSavedFieldActivity : BaseActivity() {

    private lateinit var binding: ActivityLoadSavedFieldBinding
    private val vm: LoadSavedFieldViewModel by viewModels()
    private var selectedFieldId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoadSavedFieldBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyEdgeInsets(binding.main)
        enterImmersiveMode()

        setupUI()

        // Перехват системной кнопки «Назад»
        onBackPressedDispatcher.addCallback(this) {
            finish()
        }
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
            val adapter = SavedFieldAdapter(emptyList()) { field ->
                // коллбек — сохраняем выбранный ID
                selectedFieldId = field.fieldId
            }
            rvSavedFields.apply {
                layoutManager = LinearLayoutManager(this@LoadSavedFieldActivity)
                this.adapter = adapter
            }

            // 2) Сбор списка из ViewModel
            lifecycleScope.launchWhenStarted {
                vm.fields.collect { list ->
                    adapter.submitList(list)
                }
            }

            // 3) Обработчик кнопки «Загрузить»
            btnLoad.setOnClickListener {
                selectedFieldId?.let { id ->
                    // Передаём ID в ManualPlacementActivity
                    /*startActivity(
                    Intent(this, ManualPlacementActivity::class.java)
                        .putExtra("FIELD_ID", id)
                )*/
                } ?: run {
                    // Если ничего не выбрано — предупредим пользователя
                    Snackbar.make(main, R.string.hint_select_setup, Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }
}