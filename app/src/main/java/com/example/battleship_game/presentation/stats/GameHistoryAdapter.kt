package com.example.battleship_game.presentation.stats

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.battleship_game.data.entity.GameHistory
import com.example.battleship_game.databinding.ItemGameHistoryBinding

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