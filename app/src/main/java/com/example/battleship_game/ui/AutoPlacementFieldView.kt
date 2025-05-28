package com.example.battleship_game.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View

class AutoPlacementFieldView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paintGrid = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.GRAY
        strokeWidth = 2f
    }
    private val paintShip = Paint().apply {
        style = Paint.Style.FILL
        color = Color.BLUE
    }
    private var shipPlacements: List<ShipPlacement> = emptyList()
    private var cellSize: Float = 0f

    // Обновляет список кораблей и перерисовывает вид
    fun setShipPlacements(placements: List<ShipPlacement>) {
        shipPlacements = placements
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Делаем вид квадратным (ширина = высота)
        val size = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        cellSize = (width / 10f)
        // Рисуем сетку 10x10
        for (i in 0..10) {
            canvas.drawLine(i * cellSize, 0f, i * cellSize, height.toFloat(), paintGrid)
            canvas.drawLine(0f, i * cellSize, width.toFloat(), i * cellSize, paintGrid)
        }
        // Рисуем корабли
        for (ship in shipPlacements) {
            if (ship.isHorizontal) {
                val top = ship.row * cellSize
                val left = ship.col * cellSize
                val right = left + ship.length * cellSize
                val bottom = top + cellSize
                canvas.drawRect(left, top, right, bottom, paintShip)
            } else {
                val top = ship.row * cellSize
                val left = ship.col * cellSize
                val right = left + cellSize
                val bottom = top + ship.length * cellSize
                canvas.drawRect(left, top, right, bottom, paintShip)
            }
        }
    }
}