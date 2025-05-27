package com.example.battleship_game.presentation.placement

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.battleship_game.data.entity.GamePlacement
import com.example.battleship_game.databinding.ItemSavedFieldBinding

/**
 * Адаптер для списка сохранённых расстановок (`GameField`).
 *
 * @param items начальный список (обычно пустой, затем обновляется через submitList).
 * @param onSelect вызывается, когда пользователь кликает на элемент — передаёт выбранный [GamePlacement].
 */
class SavedPlacementAdapter(
    private var items: List<GamePlacement>,
    private val onSelect: (GamePlacement) -> Unit
) : RecyclerView.Adapter<SavedPlacementAdapter.ViewHolder>() {

    private var selectedPos = RecyclerView.NO_POSITION

    /**
     * Заполняет View данными и настраивает логику выделения.
     *
     * @param item  модель расстановки
     * @param pos    её позиция в списке
     */
    inner class ViewHolder(val binding: ItemSavedFieldBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: GamePlacement, pos: Int) {
            binding.apply {
                // Устанавливаем текст в колонки
                tvName.text = item.name
                tvDate.text = item.date

                // Подсветка выделенного элемента
                root.isSelected = (pos == selectedPos)

                // Обработчик клика по строке
                root.setOnClickListener {
                    val oldPos = selectedPos
                    selectedPos = pos
                    // Обновляем старую и новую позиции, чтобы отрисовать селекцию
                    notifyItemChanged(oldPos)
                    notifyItemChanged(pos)
                    // Вызываем коллбек наружу
                    onSelect(item)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemSavedFieldBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(items[position], position)

    /**
     * Обновляет весь список и сбрасывает выделение.
     *
     * @param newItems новый список из ViewModel
     */
    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newItems: List<GamePlacement>) {
        items = newItems
        selectedPos = RecyclerView.NO_POSITION
        notifyDataSetChanged()
    }
}