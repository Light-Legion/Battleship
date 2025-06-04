package com.example.battleship_game.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.View
import com.example.battleship_game.R
import com.example.battleship_game.data.model.ShipPlacement
import kotlin.math.min

/**
 * Кастомная View для отображения игрового поля 10×10 с автоматической расстановкой кораблей.
 *
 * Особенности:
 * - Отображение сетки с координатными метками (A-J, 1-10)
 * - Визуализация кораблей с правильной ориентацией
 * - Адаптация под разные размеры экранов
 * - Оптимизированное использование памяти
 * - Центрированное расположение игрового поля
 *
 * @property context Контекст приложения
 * @property attrs Набор атрибутов из XML
 */
class AutoPlacementFieldView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    // Paint для отрисовки сетки
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.BLACK
        // Толщина линии адаптируется под плотность экрана
        strokeWidth = 1.5f * resources.displayMetrics.density
    }

    // Paint для отрисовки текстовых меток
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textAlign = Paint.Align.CENTER
    }

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.game_field) // слегка голубоватый полупрозрачный фон
        style = Paint.Style.FILL
    }

    // Кэш для загруженных и оптимизированных изображений кораблей
    private val shipBitmaps = mutableMapOf<String, Bitmap>()

    // Параметры отрисовки игрового поля
    private var cellSize = 0f    // Размер одной клетки в пикселях
    private var offsetX = 0f     // Смещение по X для начала игрового поля
    private var offsetY = 0f     // Смещение по Y для начала игрового поля
    private var gridWidth = 0f   // Ширина всей сетки (10 * cellSize)
    private var gridHeight = 0f  // Высота всей сетки (10 * cellSize)

    // Текущая расстановка кораблей на поле
    private var placements: List<ShipPlacement> = emptyList()

    init {
        val options = BitmapFactory.Options().apply {
            inScaled = true
            inDensity = DisplayMetrics.DENSITY_DEFAULT
            inTargetDensity = resources.displayMetrics.densityDpi
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        // Предзагрузка «двунаправленных» битмапов для кораблей
        val shipResources = mapOf(
            "1_h" to R.drawable.ship_horizontal_1,
            "2_h" to R.drawable.ship_horizontal_2,
            "3_h" to R.drawable.ship_horizontal_3,
            "4_h" to R.drawable.ship_horizontal_4,
            "1_v" to R.drawable.ship_vertical_1,
            "2_v" to R.drawable.ship_vertical_2,
            "3_v" to R.drawable.ship_vertical_3,
            "4_v" to R.drawable.ship_vertical_4
        )
        shipResources.forEach { (key, resId) ->
            shipBitmaps[key] = BitmapFactory.decodeResource(resources, resId, options)
        }
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
        super.onSizeChanged(w, h, oldw, oldh)

        // Учитываем padding с обеих сторон
        val horizontalPadding = paddingLeft + paddingRight
        val verticalPadding = paddingTop + paddingBottom

        // Доступное пространство для сетки (10x10 клеток)
        val availW = w - horizontalPadding
        val availH = h - verticalPadding

        // Размер клетки = мин. значение из (ширина/11, высота/11)
        cellSize = min(availW / 11f, availH / 11f)
        gridWidth = cellSize * 10f
        gridHeight = cellSize * 10f

        // Смещение для меток (левая и верхняя полоса)
        offsetX = paddingLeft + cellSize
        offsetY = paddingTop + cellSize

        val labelTextSize = cellSize * 0.5f
        textPaint.textSize = labelTextSize
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawBackground(canvas)
        drawGrid(canvas)
        drawLabels(canvas)
        drawShips(canvas)
    }

    /** Рисует полупрозрачный фон под сеткой */
    private fun drawBackground(canvas: Canvas) {
        canvas.drawRect(
            offsetX, offsetY,
            offsetX + gridWidth, offsetY + gridHeight,
            backgroundPaint
        )
    }

    /** Рисует линии сетки 10×10. */
    private fun drawGrid(canvas: Canvas) {
        for (i in 0..10) {
            val x = offsetX + i * cellSize
            val y = offsetY + i * cellSize
            canvas.drawLine(x, offsetY, x, offsetY + 10 * cellSize, gridPaint)
            canvas.drawLine(offsetX, y, offsetX + 10 * cellSize, y, gridPaint)
        }
    }

    /** Рисует цифры (1–10) сверху и буквы (A–J) слева. */
    private fun drawLabels(canvas: Canvas) {
        for (i in 0 until 10) {
            // цифра
            canvas.drawText(
                "${i + 1}",
                offsetX + (i + 0.5f) * cellSize,
                offsetY - cellSize / 3,
                textPaint
            )
            // буква
            canvas.drawText(
                ('A' + i).toString(),
                offsetX - cellSize / 2,
                offsetY + (i + 0.7f) * cellSize,
                textPaint
            )
        }
    }

    /** Рисует каждую картинку корабля, растягивая её на нужное число клеток. */
    private fun drawShips(canvas: Canvas) {
        val shipPaint = Paint().apply {
            isDither = true
            isFilterBitmap = true
        }

        placements.forEach { ship ->
            val key = "${ship.length}_${if (ship.isVertical) "v" else "h"}"
            val bitmap = shipBitmaps[key] ?: return@forEach

            val left = offsetX + ship.startCol * cellSize
            val top = offsetY + ship.startRow * cellSize
            val width = if (ship.isVertical) cellSize else ship.length * cellSize
            val height = if (!ship.isVertical) cellSize else ship.length * cellSize

            // Используем матрицу для сохранения пропорций
            val destRect = RectF(left, top, left + width, top + height)
            canvas.drawBitmap(bitmap, null, destRect, shipPaint)
        }
    }
}