package com.example.battleship_game.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.example.battleship_game.R
import com.example.battleship_game.data.model.ShipPlacement
import kotlin.math.min

/**
 * View для отображения поля 10×10 с метками и
 * автоматической расстановкой кораблей.
 */
class AutoPlacementFieldView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    //  — paint’ы для сетки и текста
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; color = Color.BLACK; strokeWidth = 2f
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK; textSize = 32f; textAlign = Paint.Align.CENTER
    }

    //  — ресурсные ID для кораблей (“{size}_{h|v}” → drawable)
    private val shipResources = mapOf(
        "1_h" to R.drawable.ship_horizontal_1,
        "2_h" to R.drawable.ship_horizontal_2,
        "3_h" to R.drawable.ship_horizontal_3,
        "4_h" to R.drawable.ship_horizontal_4,
        "1_v" to R.drawable.ship_vertical_1,
        "2_v" to R.drawable.ship_vertical_2,
        "3_v" to R.drawable.ship_vertical_3,
        "4_v" to R.drawable.ship_vertical_4
    )

    //  — кэш загруженных и down­sampled Bitmap’ов
    private val shipBitmaps = mutableMapOf<String, Bitmap>()

    //  — параметры отрисовки
    private var cellSize = 0f
    private var offsetX = 0f
    private var offsetY = 0f

    //  — текущая расстановка
    private var placements: List<ShipPlacement> = emptyList()

    init {
        // Предзагружаем все битмапы один раз
        preloadShipBitmaps()
    }

    /**
     * Загружает из ресурсов все битмапы для кораблей,
     * автоматически рассчитывая downSampling по размеру на экране.
     */
    private fun preloadShipBitmaps() {
        // предположим, что cellSize ещё не вычислен — используем density, потом ресайзим при рисовании
        val metrics = resources.displayMetrics
        val target = (metrics.density * 48).toInt() // ориентирамся на 48dp

        shipResources.forEach { (key, resId) ->
            // 1) Узнаём размеры
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeResource(resources, resId, opts)
            val scale = calculateInSampleSize(opts.outWidth, opts.outHeight, target, target)

            // 2) Загружаем с inSampleSize
            opts.inJustDecodeBounds = false
            opts.inSampleSize = scale
            opts.inScaled = false

            val bmp = BitmapFactory.decodeResource(resources, resId, opts)
            shipBitmaps[key] = bmp
        }
    }

    /** Алгоритм подбора inSampleSize из документации Android. */
    private fun calculateInSampleSize(
        width: Int, height: Int,
        reqWidth: Int, reqHeight: Int
    ): Int {
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfH = height / 2
            val halfW = width  / 2
            while (halfH / inSampleSize >= reqHeight && halfW / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    /** Освобождаем память при удалении View. */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        shipBitmaps.values.forEach { it.recycle() }
        shipBitmaps.clear()
    }

    /** Вызов из Activity, чтобы передать новую расстановку. */
    fun setPlacements(list: List<ShipPlacement>) {
        placements = list
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        // высчитываем cellSize так, чтобы 10 клеток + метки помещались
        val availW = w - paddingLeft - paddingRight
        val availH = h - paddingTop - paddingBottom
        cellSize = min(availW / 11f, availH / 11f)
        offsetX = paddingLeft + cellSize
        offsetY = paddingTop  + cellSize
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawGrid(canvas)
        drawLabels(canvas)
        drawShips(canvas)
    }

    /** Рисует линии сетки 10×10. */
    private fun drawGrid(canvas: Canvas) {
        for (i in 0..10) {
            val x = offsetX + i * cellSize
            val y = offsetY + i * cellSize
            canvas.drawLine(x, offsetY, x, offsetY + 10*cellSize, gridPaint)
            canvas.drawLine(offsetX, y, offsetX + 10*cellSize, y, gridPaint)
        }
    }

    /** Рисует цифры (1–10) сверху и буквы (A–J) слева. */
    private fun drawLabels(canvas: Canvas) {
        for (i in 0 until 10) {
            // цифра
            canvas.drawText("${i+1}",
                offsetX + (i+0.5f)*cellSize,
                offsetY - cellSize/3,
                textPaint)
            // буква
            canvas.drawText(('A'+i).toString(),
                offsetX - cellSize/2,
                offsetY + (i+0.7f)*cellSize,
                textPaint)
        }
    }

    /** Рисует каждую картинку корабля, растягивая её на нужное число клеток. */
    private fun drawShips(canvas: Canvas) {
        for (s in placements) {
            val key = "${s.length}_${if (s.isVertical) "v" else "h"}"
            val bmp = shipBitmaps[key] ?: continue

            val left   = offsetX + s.startCol * cellSize
            val top    = offsetY + s.startRow * cellSize
            val right  = left + if (s.isVertical) cellSize else s.length * cellSize
            val bottom = top  + if (!s.isVertical) cellSize else s.length * cellSize

            canvas.drawBitmap(bmp, null, RectF(left, top, right, bottom), null)
        }
    }
}