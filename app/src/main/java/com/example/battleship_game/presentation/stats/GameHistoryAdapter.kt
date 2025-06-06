package com.example.battleship_game.presentation.stats

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.battleship_game.R
import com.example.battleship_game.data.entity.GameHistory
import com.example.battleship_game.data.model.Difficulty
import com.example.battleship_game.data.model.GameResult
import com.example.battleship_game.databinding.ItemGameHistoryBinding
import com.google.android.material.color.MaterialColors

/**
 * Адаптер для RecyclerView статистики.
 */
class GameHistoryAdapter(
    private var items: List<GameHistory>
) : RecyclerView.Adapter<GameHistoryAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemGameHistoryBinding)
        : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: GameHistory) {
            binding.apply {
                val context = root.context
                tvName.text = item.name
                tvResult.text = context.getString(item.result.displayNameRes)
                tvLevel.text = context.getString(item.level.displayNameRes)
                tvDate.text = item.date

                // Устанавливаем золотой фон для побед на экспертном уровне
                if (item.level == Difficulty.EXPERT && item.result == GameResult.WIN) {
                    cardGameHistory.apply {
                        setCardBackgroundColor(ContextCompat.getColor(context, R.color.gold))
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemGameHistoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(items[position])

    /**
     * Обновить список (например, при приходе данных из ViewModel).
     */
    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newItems: List<GameHistory>) {
        items = newItems
        notifyDataSetChanged()
    }
}