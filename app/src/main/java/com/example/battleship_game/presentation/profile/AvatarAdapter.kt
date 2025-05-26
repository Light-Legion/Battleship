package com.example.battleship_game.presentation.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.battleship_game.databinding.ItemAvatarBinding

/**
 * Адаптер для горизонтального списка аватаров.
 * - items: список drawable-ресурсов
 * - selected: индекс текущего выбранного аватара
 * - onSelect: callback при выборе нового
 */
class AvatarAdapter(
    private val items: List<Int>,
    private var selected: Int,
    private val onSelect: (Int) -> Unit
) : RecyclerView.Adapter<AvatarAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemAvatarBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(pos: Int) {
            binding.apply {
                // 1) Устанавливаем картинку:
                ivAvatar.setImageResource(items[pos])

                // 2) Подсвечиваем рамку, если этот элемент выбран:
                val color = if (pos == selected)
                    android.R.color.holo_blue_light
                else android.R.color.transparent
                cardAvatar.strokeColor = root.context.getColor(color)

                // 3) Обработчик клика:
                root.setOnClickListener {
                    val old = selected
                    selected = pos
                    // обновляем старый и новый элемент, чтобы перерисовать рамки
                    notifyItemChanged(old)
                    notifyItemChanged(pos)
                    // сообщаем Activity о смене
                    onSelect(pos)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) : ViewHolder =
        ViewHolder(
            ItemAvatarBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun getItemCount() = items.size
    override fun onBindViewHolder(h: ViewHolder, p: Int) = h.bind(p)
}
