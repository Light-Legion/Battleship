package com.example.battleship_game.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Point
import android.graphics.RectF
import android.view.View

// ─── Вспомогательный класс для рисования тени именно нужного корабля ─────────

/**
 * DragShadowBuilder, рисующий только bitmap одного корабля,
 * размером (shadowWidth × shadowHeight) и точкой касания (touchX, touchY).
 */
class ShipDragShadowBuilder(
    private val bitmap: Bitmap,
    private val shadowWidth: Int,
    private val shadowHeight: Int,
    private val touchX: Int,
    private val touchY: Int
) : View.DragShadowBuilder() {

    override fun onProvideShadowMetrics(size: Point, touch: Point) {
        // Размер тени = размер корабля (shadowWidth × shadowHeight)
        size.set(shadowWidth, shadowHeight)
        // Точка касания внутри тени = (touchX, touchY)
        touch.set(touchX, touchY)
    }

    override fun onDrawShadow(canvas: Canvas) {
        // Рисуем bitmap корабля во всю область тени
        val dest = RectF(0f, 0f, shadowWidth.toFloat(), shadowHeight.toFloat())
        canvas.drawBitmap(bitmap, null, dest, null)
    }
}