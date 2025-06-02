package com.example.battleship_game.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import com.example.battleship_game.R
import com.example.battleship_game.data.model.ShipPlacement
import kotlin.math.min

/**
 * View для ручной расстановки кораблей на поле «Морской бой».
 *
 * Поддерживает:
 *  1. Drag&Drop из шаблонов (externalDragStart)
 *  2. Проверку границ и коллизий
 *  3. Подсветку конфликтующих красной свечой
 *  4. Одиночный тап — выбор + запрос поворота
 *  5. Двойной тап — мгновенный поворот
 *  6. Shake-анимация при ошибке
 *  7. Удаление за пределами поля
 *
 * Чтобы получать коллбэки, реализуйте [Listener] в Activity/Fragment.
 */
class ManualPlacementFieldView @JvmOverloads constructor(
    ctx: Context, attrs: AttributeSet? = null
) : View(ctx, attrs) {

    companion object {
        private const val GRID_SIZE = 10
    }

    // Размер клетки и отступы для выравнивания
    private var cellSize = 0f
    private var offsetX = 0f
    private var offsetY = 0f

    // Рисовалки
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        strokeWidth = 2f
    }
    private val conflictPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 6f
        maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.OUTER)
    }

    // Спрайты (подмените на свои R.drawable.ship_vert/horiz)
    private val bmpVert = BitmapFactory.decodeResource(resources, R.drawable.ic_launcher_background)
    private val bmpHoriz = BitmapFactory.decodeResource(resources, R.drawable.ic_launcher_foreground)

    // Состояние поля
    private val ships = mutableListOf<ShipPlacement>()
    private var dragging: ShipPlacement? = null
    private var dragOffsetX = 0f
    private var dragOffsetY = 0f
    private var selected: ShipPlacement? = null

    // Детектор жестов
    private val gesture = GestureDetector(ctx, GestureListener())

    /** Обратный интерфейс для Activity/Fragment */
    interface Listener {
        /** Включать/отключать кнопку «В бой!». */
        fun onFieldValidityChanged(isValid: Boolean)
        /** Показать FAB или сразу поворачивать. */
        fun onRotateRequested(ship: ShipPlacement)
    }
    var listener: Listener? = null

    // ─── Изменение размеров ────────────────────────────────────

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        // Подгоняем размер под квадрат 0.8 от меньшей стороны
        val size = min(w, h) * 0.8f
        cellSize = size / GRID_SIZE
        offsetX = (w - size) / 2f
        offsetY = (h - size) / 2f
    }

    // ─── Рисуем сетку и корабли ───────────────────────────────

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawGrid(canvas)
        drawShips(canvas)
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = cellSize * 0.4f
        textAlign = Paint.Align.CENTER
    }

    /** Сетка 10×10 линиями */
    private fun drawGrid(canvas: Canvas) {
        for (i in 0..GRID_SIZE) {
            val x = offsetX + i * cellSize
            canvas.drawLine(x, offsetY, x, offsetY + GRID_SIZE * cellSize, gridPaint)
            val y = offsetY + i * cellSize
            canvas.drawLine(offsetX, y, offsetX + GRID_SIZE * cellSize, y, gridPaint)

            // подпись сверху (буквы A–J)
            if (i < GRID_SIZE) {
                val x = offsetX + (i + 0.5f) * cellSize
                val y = offsetY - cellSize * 0.2f
                canvas.drawText(('A' + i).toString(), x, y, textPaint)
            }
            // подпись слева (цифры 1–10)
            if (i < GRID_SIZE) {
                val x = offsetX - cellSize * 0.3f
                val y = offsetY + (i + 0.75f) * cellSize
                canvas.drawText((i + 1).toString(), x, y, textPaint)
            }
        }
    }

    /** Корабли: спрайт, подсветка конфликтов, иконка поворота */
    private fun drawShips(canvas: Canvas) {
        ships.forEach { ship ->
            val rect = computeRect(ship)
            /*if (ship.hasConflict) {
                canvas.drawRect(rect, conflictPaint)
            }*/
            val bmp = if (ship.isVertical) bmpVert else bmpHoriz
            canvas.drawBitmap(bmp, null, rect, null)

            if (ship == selected) {
                // Рисуем иконку поворота в углу rect
                val icon = resources.getDrawable(R.drawable.ic_launcher_foreground, null)
                val size = cellSize * 0.6f
                icon.setBounds(
                    (rect.right - size).toInt(), rect.top.toInt(),
                    rect.right.toInt(), (rect.top + size).toInt()
                )
                icon.draw(canvas)
            }
        }
    }

    /** Экранный прямоугольник корабля */
    private fun computeRect(ship: ShipPlacement): RectF {
        val x0 = offsetX + ship.startCol * cellSize
        val y0 = offsetY + ship.startRow * cellSize
        val w = if (ship.isVertical) cellSize else ship.length * cellSize
        val h = if (ship.isVertical) ship.length * cellSize else cellSize
        return RectF(x0, y0, x0 + w, y0 + h)
    }

    // ─── Обработка касаний ────────────────────────────────────

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        // Жесты (double tap / single tap)
        if (gesture.onTouchEvent(ev)) return true

        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // Начинаем drag, если тапнули по кораблю
                ships.find { computeRect(it).contains(ev.x, ev.y) }?.let { ship ->
                    dragging = ship
                    selected = null
                    val r = computeRect(ship)
                    dragOffsetX = ev.x - r.left
                    dragOffsetY = ev.y - r.top
                }
            }
            MotionEvent.ACTION_MOVE -> {
                /*dragging?.let { ship ->
                    ship.tempX = ev.x - offsetX - ship.startCol * cellSize - dragOffsetX
                    ship.tempY = ev.y - offsetY - ship.startRow * cellSize - dragOffsetY
                }*/
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                dragging?.also { finalizeDrop(it) }
                dragging = null
            }
        }
        invalidate()
        return true
    }

    /** Завершение дропа: ставим, удаляем или откатываем. */
    private fun finalizeDrop(ship: ShipPlacement) {
        val newCol = ((ship.startCol * cellSize) / cellSize).toInt()
        val newRow = ((ship.startRow * cellSize) / cellSize).toInt()

        // Удаление, если бросили за борт
        if (newCol !in 0 until GRID_SIZE || newRow !in 0 until GRID_SIZE) {
            ships.remove(ship)
            /*ship.tempX = 0f; ship.tempY = 0f*/
            notifyValidity()
            return
        }

        // Попытка установить
        if (tryPlace(ship, newRow, newCol)) {
            ship.startRow = newRow
            ship.startCol = newCol
            /*ship.tempX = 0f; ship.tempY = 0f*/
        } else {
            // Ошибка — дрожь и сброс позиции
            shake {
                /*ship.tempX = 0f; ship.tempY = 0f*/
                invalidate()
            }
        }
        notifyValidity()
    }

    /** Пробуем разместить: границы + коллизии. */
    private fun tryPlace(ship: ShipPlacement, row: Int, col: Int): Boolean {
        // Проверка по границам
        if (ship.isVertical && row + ship.length > GRID_SIZE) return false
        if (!ship.isVertical && col + ship.length > GRID_SIZE) return false

        // Сбросим конфликты
        /*ships.forEach { it.hasConflict = false }*/

        // Проверка пересечений
        var ok = true
        ships.forEach { other ->
            if (other !== ship && intersects(ship, row, col, other)) {
                /*ship.hasConflict = true
                other.hasConflict = true*/
                ok = false
            }
        }
        return ok
    }

    /** Проверка пересечения двух прямоугольников кораблей */
    private fun intersects(
        s1: ShipPlacement, r: Int, c: Int, s2: ShipPlacement
    ): Boolean {
        val a = RectF(
            offsetX + c * cellSize,
            offsetY + r * cellSize,
            offsetX + c * cellSize + if (s1.isVertical) cellSize else s1.length * cellSize,
            offsetY + r * cellSize + if (s1.isVertical) s1.length * cellSize else cellSize
        )
        val b = computeRect(s2)
        return RectF.intersects(a, b)
    }

    /** Анимация дрожания при неуспехе */
    private fun shake(onEnd: () -> Unit) {
        ObjectAnimator.ofFloat(this, "translationX", 0f, 20f).apply {
            duration = 300
            interpolator = android.view.animation.CycleInterpolator(5f)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) = onEnd()
            })
            start()
        }
    }

    // ─── Публичный API для Activity ───────────────────────────

    /**
     * Начинает drag нового корабля.
     *
     * @param rawX мировая X координата тача (ev.rawX).
     * @param rawY мировая Y координата тача (ev.rawY).
     * @param ship  экземпляр [ShipPlacement], скопированный из шаблона.
     */
    fun externalDragStart(rawX: Float, rawY: Float, ship: ShipPlacement) {
        // Сбрасываем temp и флаги
        /*ship.tempX = 0f
        ship.tempY = 0f
        ship.hasConflict = false*/
        ships += ship

        // Рассчитываем dragOffset так, чтобы спрайт «прилип»
        // rawX/rawY — глобальные, нужно перевести в локальные
        val loc = IntArray(2)
        getLocationOnScreen(loc)
        dragOffsetX = rawX - loc[0] - (offsetX + ship.startCol * cellSize)
        dragOffsetY = rawY - loc[1] - (offsetY + ship.startRow * cellSize)
        dragging = ship
        invalidate()
    }

    /** Удалить все корабли с поля. */
    fun clearAll() {
        ships.clear()
        selected = null
        invalidate()
        notifyValidity()
    }

    /** Получить список текущих расстановок для сохранения или передачи. */
    fun getPlacedShips(): List<ShipPlacement> =
        ships.map { it.copy() }  // возвращаем копии

    /** Повернуть выбранный корабль (например, по запросу FAB). */
    fun rotateSelected() {
        selected?.let { ship ->
            ship.isVertical = !ship.isVertical
            // проверяем, валидно ли после поворота
            if (!tryPlace(ship, ship.startRow, ship.startCol)) {
                shake { /* позиция не меняется */ }
            }
            invalidate()
            notifyValidity()
        }
    }

    /** Проверяем и шлём коллбэк валидности поля */
    private fun notifyValidity() {
        val ok = ships.size == 10 && ships.all { !it.isVertical }
        listener?.onFieldValidityChanged(ok)
    }

    // ─── Детектор одиночных/двойных тапов ───────────────────────

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        /** Выбор и запрос показать кнопку поворота. */
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            selected = ships.find { computeRect(it).contains(e.x, e.y) }
            selected?.let { listener?.onRotateRequested(it) }
            invalidate()
            return true
        }
        /** Мгновенный поворот по двойному тапу. */
        override fun onDoubleTap(e: MotionEvent): Boolean {
            ships.find { computeRect(it).contains(e.x, e.y) }?.let { ship ->
                ship.isVertical = !ship.isVertical
                if (!tryPlace(ship, ship.startRow, ship.startCol)) {
                    shake { /* остаётся старое положение */ }
                }
                invalidate()
                notifyValidity()
            }
            return true
        }
        override fun onDown(e: MotionEvent) = true
    }
}
