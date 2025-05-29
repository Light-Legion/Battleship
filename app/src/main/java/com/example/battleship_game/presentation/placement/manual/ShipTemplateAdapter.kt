package com.example.battleship_game.presentation.placement.manual

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.battleship_game.data.model.ShipPlacement
import com.example.battleship_game.databinding.ItemShipTemplateBinding

/**
 * Adapter для шаблонов кораблей.
 *
 * @param items Список шаблонов.
 * @param onTemplateDrag Коллбэк, вызываемый при ACTION_DOWN:
 *        rawX и rawY — глобальные координаты тача,
 *        ship — копия шаблона для перетаскивания.
 */
class ShipTemplateAdapter(
    private val items: List<ShipPlacement>,
    private val onTemplateDrag: (ship: ShipPlacement, rawX: Float, rawY: Float) -> Unit
) : RecyclerView.Adapter<ShipTemplateAdapter.ViewHolder>() {

    inner class ViewHolder(private val vb: ItemShipTemplateBinding) :
        RecyclerView.ViewHolder(vb.root) {

        @SuppressLint("ClickableViewAccessibility")
        fun bind(ship: ShipPlacement) {
            // Ширина = length × 24dp
            val dp = vb.root.context.resources.displayMetrics.density
            vb.root.layoutParams.width = (dp * ship.length * 24).toInt()
            vb.root.requestLayout()

            // TOUCH_DOWN → запускаем drag
            vb.root.setOnTouchListener { _, ev ->
                if (ev.action == MotionEvent.ACTION_DOWN) {
                    // Передаем копию модели, чтобы оригинал не изменялся
                    onTemplateDrag(ship.copy(), ev.rawX, ev.rawY)
                    true
                } else {
                    false
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemShipTemplateBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }
}