package com.example.battleship_game.presentation.stats

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.battleship_game.data.entity.GameProgress
import com.example.battleship_game.databinding.ItemGameProgressBinding

/**
 * Адаптер для RecyclerView статистики.
 */
class GameProgressAdapter(
    private var items: List<GameProgress>
) : RecyclerView.Adapter<GameProgressAdapter.ViewHolder>() {

    inner class ViewHolder(private val vb: ItemGameProgressBinding)
        : RecyclerView.ViewHolder(vb.root) {
        fun bind(item: GameProgress) {
            vb.tvName.text   = item.name
            vb.tvResult.text = item.result.toDisplayString()
            vb.tvLevel.text  = item.level.toDisplayString()
            vb.tvDate.text   = item.date
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemGameProgressBinding.inflate(
            LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(items[position])

    /**
     * Обновить список (например, при приходе данных из ViewModel).
     */
    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newItems: List<GameProgress>) {
        items = newItems
        notifyDataSetChanged()
    }
}