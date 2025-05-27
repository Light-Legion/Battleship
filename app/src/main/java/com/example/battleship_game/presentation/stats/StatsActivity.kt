package com.example.battleship_game.presentation.stats

import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.battleship_game.common.BaseActivity
import com.example.battleship_game.databinding.ActivityStatsBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Activity статистики.
 * Отображает кнопку "Назад" и таблицу с результатами игр.
 */
class StatsActivity : BaseActivity() {

    private lateinit var binding: ActivityStatsBinding
    private val vm: StatsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityStatsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyEdgeInsets(binding.main)
        enterImmersiveMode()

        setupUI()
        observeStats()

        // Перехват системной кнопки «Назад»
        onBackPressedDispatcher.addCallback(this) {
            finish()
        }
    }

    private fun setupUI() {
        // Назад
        binding.btnBack.setOnClickListener { finish() }

        // RecyclerView
        binding.rvStats.apply {
            layoutManager = LinearLayoutManager(this@StatsActivity)
            adapter = GameHistoryAdapter(emptyList())
        }
    }

    /**
     * Подписка на поток статистики из ViewModel.
     * Используем lifecycleScope + repeatOnLifecycle,
     * чтобы сборка происходила только когда Activity в STARTED.
     */
    private fun observeStats() {
        val adapter = (binding.rvStats.adapter as GameHistoryAdapter)

        lifecycleScope.launch {
            // Повторяем сбор, когда статус ≥ STARTED
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.stats
                    .collectLatest { list ->
                        adapter.submitList(list)
                    }
            }
        }
    }
}