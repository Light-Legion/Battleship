package com.example.battleship_game.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.ClipData
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
import android.view.DragEvent
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import com.example.battleship_game.R
import com.example.battleship_game.data.model.ShipPlacementUi
import kotlin.math.abs
import kotlin.math.min

/**
 * Custom View для экрана «Ручная расстановка кораблей».
 *
 * Содержит:
 * 1) Отрисовку 10×10 сетки, меток (A–J, 1–10).
 * 2) Отрисовку всех кораблей [ShipPlacementUi] через updateShips(...).
 * 3) Drag-and-drop: слушает события drag, при ACTION_DROP вычисляет, попал ли в клетку [row,col].
 *    Если попал → onShipDropped(ship, row, col), иначе → onShipDroppedOutside(ship).
 * 4) Double-tap: через GestureDetector, при нахождении корабля под тапом → onShipDoubleTapped(ship).
 * 5) Подсветка «неправильных» клеток под ship.isInvalid==true с помощью invalidCellPaint.
 * 6) Shake-анимация при ошибочном повороте (вызов animateShake(...)).
 */
class ManualPlacementFieldView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null
) : View(ctx, attrs) {

    /** Слушатель для событий drop-а корабля на поле */
    interface OnShipDropListener {
        /**
         * Вызывается при дропе корабля на игровое поле над клеткой [row], [col].
         * [ship] – объект ShipPlacementUi, ранее заданный Activity (fromTemplate=true или уже размещённый).
         */
        fun onShipDropped(ship: ShipPlacementUi, row: Int, col: Int)

        /**
         * Вызывается, когда дроп завершился вне игрового поля (некорректный дроп).
         * [ship] – тот же объект ShipPlacementUi, который пытались дропнуть.
         * Activity должна вернуть этот корабль обратно на станцию (сделать View видимым).
         */
        fun onShipDroppedOutside(ship: ShipPlacementUi)
    }

    /** Слушатель для двойного тапа по уже размещённому кораблю */
    interface OnShipDoubleTapListener {
        /**
         * Вызывается, когда пользователь сделал двойной тап внутри клетки одного из размещённых кораблей.
         * [ship] – объект ShipPlacementUi, который нужно попытаться повернуть.
         * Activity/ViewModel сами решают, удачна ли поворотная операция. */
        fun onShipDoubleTapped(ship: ShipPlacementUi)
    }

    // ======== Константы для размеров поля ========
    companion object {
        private const val TAG = "MPFieldView"
        private const val GRID_SIZE = 10               // 10×10 клеток
        private const val INVALID_SCALE = 0.9f         // Масштабирование для некорректного размещения
        private const val INVALID_ALPHA = 150          // Прозрачность для подсветки некорректных (0–255)
    }

    // ======== Paint-объекты для рисования ========

    /** Линии сетки (чёрные) */
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.BLACK
        strokeWidth = 1.5f * resources.displayMetrics.density
    }

    /** Полупрозрачный фон «игрового поля» (светло-голубой) */
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.game_field)
        style = Paint.Style.FILL
    }

    /** Текст (метки столбцов/строк) */
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textAlign = Paint.Align.CENTER
    }

    /** Paint для заливки «неправильных» клеток (красный, полупрозрачный) */
    private val invalidCellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.invalidate_ship)
        style = Paint.Style.FILL
        alpha = INVALID_ALPHA
    }

    // Новый Paint для пастельного зелёного фона ячейки
    private val hoverPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.pointer_cell) // например: #A5D6A7
        style = Paint.Style.FILL
        alpha = 120 // чуть полупрозрачный
    }

    // ======== Битмапы для кораблей (гориз./верт.) ========

    /** Кэш загруженных картинок кораблей: key = "${length}_${orientation}", где orientation: "h" или "v" */
    private val shipBitmaps: MutableMap<String, Bitmap> = mutableMapOf()

    // ======== Параметры отрисовки поля ========

    /** Размер одной клетки в пикселях (устанавливается в onSizeChanged) */
    private var cellSize = 0f

    /** Ширина/высота всей сетки (10 × cellSize) */
    private var gridWidth = 0f
    private var gridHeight = 0f

    /** Сдвиг по X и Y для начала сетки (учитывая padding и область для меток) */
    private var offsetX = 0f
    private var offsetY = 0f

    // ======== Состояние кораблей ========

    /**
     * Список всех кораблей, которые сейчас «живут» на поле (как шаблоны, так и уже размещённые).
     * – Если ShipPlacementUi.fromTemplate=true, значит этот корабль ещё не ставился, это шаблон.
     * – Если fromTemplate=false, значит этот корабль уже перенесён на поле (либо загружен из базы).
     * – После дропа корабля локально обновляется его startRow/startCol и fromTemplate=false → перерисовка.
     * – Если при дропе получилась «буферная» (нефатальная) коллизия, то ship.isInvalid=true → рисуем уменьшенным и
     *   с красным фоном.
     * – Если коллизия «палубная» (фатальная), то произойдёт возврат на станцию: вызываем onShipDroppedOutside.
     */
    private var placements: MutableList<ShipPlacementUi> = mutableListOf()

    // ======== Hover: текущая ячейка, над которой курсор во время drag ========

    private var hoverCell: Pair<Int, Int>? = null

    // Текущие смещения тени по X и Y (чтобы знать, где сверху-лево тени относительно пальца)
    private var shadowOffsetX = 0f
    private var shadowOffsetY = 0f

    // ======== Shake (смещения) для shipId ========

    private var shakingShipId: Int? = null
    private var shakeOffset = 0f

    // ======== Слушатели (Activity задаёт их извне) ========

    private var dropListener: OnShipDropListener? = null
    private var doubleTapListener: OnShipDoubleTapListener? = null

    /**
     * Колбэк, вызываемый при ACTION_DOWN на уже размещённом корабле.
     * Activity подвяжет к нему логику возврата корабля на станцию.
     * Если null → значит переноска с поля не поддерживается.
     */
    var onShipPickedFromField: ((ShipPlacementUi) -> Unit)? = null

    // ======== GestureDetector для распознавания double-tap ========

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            // При двойном тапе: нужно понять, попал ли пользователь в какой-нибудь корабль.
            val tappedShip = findShipAt(e.x, e.y)
            tappedShip?.let { ship ->
                Log.d(TAG, "DoubleTap по кораблю id=${ship}")
                // Передаём событие в Activity/ViewModel
                doubleTapListener?.onShipDoubleTapped(ship)
            }
            return true
        }
    })

    // ─── Поля для drag‐логики ────────────────────────────────────────────────────

    /** На каком корабле мы “стояли” при ACTION_DOWN, но ещё не начали drag */
    private var selectedShip: ShipPlacementUi? = null

    /** Системный порог, чтобы понять, что пользователь действительно двигает палец */
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    /** Координаты первого касания (для вычисления смещения) */
    private var downX = 0f
    private var downY = 0f

    /** Флаг: drag уже был запущен (чтобы не стартовать его несколько раз) */
    private var dragStarted = false

    // Чтобы не вызывать onShipPickedFromField дважды
    private var alreadyPickedShipId: Int? = null

    init {
        // Заранее загружаем битмапы кораблей (гориз./верт.) в кэш
        val options = BitmapFactory.Options().apply {
            inScaled = true
            inDensity = DisplayMetrics.DENSITY_DEFAULT
            inTargetDensity = resources.displayMetrics.densityDpi
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val resourcesMap = mapOf(
            "1_h" to R.drawable.ship_horizontal_1,
            "2_h" to R.drawable.ship_horizontal_2,
            "3_h" to R.drawable.ship_horizontal_3,
            "4_h" to R.drawable.ship_horizontal_4,
            "1_v" to R.drawable.ship_vertical_1,
            "2_v" to R.drawable.ship_vertical_2,
            "3_v" to R.drawable.ship_vertical_3,
            "4_v" to R.drawable.ship_vertical_4
        )
        resourcesMap.forEach { (key, resId) ->
            shipBitmaps[key] = BitmapFactory.decodeResource(resources, resId, options)
        }

        // Навешиваем OnDragListener на саму View, чтобы принимать drag-события
        setOnDragListener { _, event ->
            when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> {
                    val ship = event.localState as? ShipPlacementUi

                    Log.d(TAG, "ACTION_DRAG_STARTED, localState=${event.localState}")
                    // При старте drag-operaton проверяем, что у нас в ClipDescription есть нужный MIME-тип
                    if (ship != null && alreadyPickedShipId != ship.shipId) {
                        alreadyPickedShipId = ship.shipId
                        onShipPickedFromField?.invoke(ship)
                    }
                    return@setOnDragListener (ship != null)
                }

                DragEvent.ACTION_DRAG_ENTERED -> {
                    Log.d(TAG, "ACTION_DRAG_ENTERED")
                    // Можно подсветить поле, например, изменение цвета фона (по желанию)
                    invalidate()
                    return@setOnDragListener true
                }

                DragEvent.ACTION_DRAG_LOCATION -> {
                    // Вычисляем «adjusted» координаты для верхнего левого угла тени:
                    handleHover(event)
                    return@setOnDragListener true
                }

                DragEvent.ACTION_DRAG_EXITED -> {
                    Log.d(TAG, "ACTION_DRAG_EXITED")
                    // курсор вышел за границы поля
                    resetHover()
                    return@setOnDragListener true
                }

                DragEvent.ACTION_DROP -> {
                    // Сбросим hover
                    Log.d(TAG, "ACTION_DROP, localState=${event.localState}, event.x=${event.x}, event.y=${event.y}")
                    resetHover()
                    // Получим ship из localState
                    val ship = event.localState as? ShipPlacementUi
                    if (ship != null) {
                        // Преобразуем координаты (учтя смещение тени!)
                        val adjX = event.x - shadowOffsetX
                        val adjY = event.y - shadowOffsetY

                        val col = ((adjX - offsetX) / cellSize).toInt()
                        val row = ((adjY - offsetY) / cellSize).toInt()

                        if (row in 0 until GRID_SIZE && col in 0 until GRID_SIZE) {
                            Log.d(TAG, "→ onShipDropped(shipId=${ship.shipId}, row=$row, col=$col)")
                            dropListener?.onShipDropped(ship, row, col)
                        } else {
                            Log.d(TAG, "→ onShipDroppedOutside(shipId=${ship.shipId}) (за пределами)")
                            dropListener?.onShipDroppedOutside(ship)
                        }
                    }
                    return@setOnDragListener true
                }

                DragEvent.ACTION_DRAG_ENDED -> {
                    // Если drop не завершён (event.result==false), то вызываем onShipDroppedOutside
                    val ship = event.localState as? ShipPlacementUi
                    Log.d(TAG, "ACTION_DRAG_ENDED, result=${event.result}, wasDropped=${event.result}, ship=$ship")
                    if (ship != null && !event.result) {
                        Log.d(TAG, "→ onShipDroppedOutside(shipId=${ship.shipId}) (не было ACTION_DROP)")
                        dropListener?.onShipDroppedOutside(ship)
                    }
                    resetHover()
                    alreadyPickedShipId = null
                    return@setOnDragListener true
                }
                else -> return@setOnDragListener false
            }
        }
    }

    private fun handleHover(event : DragEvent) {
        val adjX = event.x - shadowOffsetX
        val adjY = event.y - shadowOffsetY
        val col = ((adjX - offsetX) / cellSize).toInt()
        val row = ((adjY - offsetY) / cellSize).toInt()
        val newHover = if (row in 0 until GRID_SIZE && col in 0 until GRID_SIZE) {
            row to col
        } else null
        if (newHover != hoverCell) {
            hoverCell = newHover
            invalidate()
        }
    }

    private fun resetHover() {
        if (hoverCell != null) {
            hoverCell = null
            invalidate()
        }
    }

    /**
     * Устанавливает смещение тени (OffsetDragShadowBuilder) столько-то px вправо/вниз.
     * Передаётся из Activity сразу после startDragAndDrop().
     */
    fun setDragShadowOffset(dx: Float, dy: Float) {
        shadowOffsetX = dx
        shadowOffsetY = dy
    }

    /** Установка внешнего слушателя drop-событий */
    fun setOnShipDropListener(listener: OnShipDropListener) {
        dropListener = listener
    }

    /** Установка внешнего слушателя двойного тапа */
    fun setOnShipDoubleTapListener(listener: OnShipDoubleTapListener) {
        doubleTapListener = listener
    }

    // ======== Методы для обновления состояния кораблей из Activity/ViewModel ========

    /**
     * Вызывается из Activity каждый раз, когда изменился список [ShipPlacementUi] (LiveData).
     * Обновляем локальный список и перерисовываем.
     */
    fun updateShips(newList: List<ShipPlacementUi>) {
        placements = newList.toMutableList()
        invalidate()
    }

    /**
     * Анимация «дрожания» только одного ship (с vehicleId).
     * Мы «трясем» его по горизонтали (±10% от cellSize) несколько раз.
     */
    fun animateShipShake(ship: ShipPlacementUi) {
        // Останавливаем предыдущую анимацию для этого корабля
        shakingShipId?.takeIf { it == ship.shipId }?.let { return }

        shakingShipId = ship.shipId
        ship.isVertical = !ship.isVertical // Мгновенно меняем ориентацию

        ValueAnimator.ofFloat(-0.1f, 0.1f).apply {
            duration = 80
            repeatCount = 5
            repeatMode = ValueAnimator.REVERSE
            interpolator = LinearInterpolator()

            addUpdateListener {
                shakeOffset = cellSize * (it.animatedValue as Float)
                invalidate()
            }

            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    ship.isVertical = !ship.isVertical // Возвращаем ориентацию
                    shakingShipId = null
                    shakeOffset = 0f
                    invalidate()
                }
            })
        }.start()
    }

    // ======== Переопределённые методы жизненного цикла View ========

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // 1) Сначала передаём жесты GestureDetector для обработки double‐tap:
        if (gestureDetector.onTouchEvent(event)) {
            return true
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // 2) Запоминаем, на каком корабле стоит палец (если стоит)
                val ship = findShipAt(event.x, event.y)
                if (ship != null && !ship.fromTemplate) {
                    selectedShip = ship
                    downX = event.x
                    downY = event.y
                    dragStarted = false
                    Log.d(TAG, "ACTION_DOWN по кораблю id=${ship.shipId} at x=${event.x}, y=${event.y}")
                    // Возвращаем true, чтобы начать получать ACTION_MOVE
                    return true
                }
            }

            MotionEvent.ACTION_MOVE -> {
                val ship = selectedShip
                if (ship != null && !dragStarted) {
                    val dx = abs(event.x - downX)
                    val dy = abs(event.y - downY)
                    if (dx > touchSlop || dy > touchSlop) {
                        Log.d(TAG, "ACTION_MOVE: начинаем drag для shipId=${ship.shipId}")

                        // Берём bitmap и размер корабля из кэша
                        val key = "${ship.length}_${if (ship.isVertical) "v" else "h"}"
                        val bmp = shipBitmaps[key] ?: return true

                        // Размеры тени:
                        val shadowW = if (ship.isVertical) cellSize.toInt() else (ship.length * cellSize).toInt()
                        val shadowH = if (ship.isVertical) (ship.length * cellSize).toInt() else cellSize.toInt()

                        // Сдвигаем тень вверх/влево ровно на 1.5 cellSize,
                        // чтобы нос корабля оказался слева/сверху от пальца на 1 клетку
                        val touchX = (cellSize / 2).toInt()
                        val touchY = (cellSize / 2).toInt()

                        val shadow = ShipDragShadowBuilder(bmp, shadowW, shadowH, touchX, touchY)
                        setDragShadowOffset(touchX.toFloat(), touchY.toFloat())

                        val dummyClip = ClipData.newPlainText("", "")
                        val flags = DRAG_FLAG_GLOBAL or DRAG_FLAG_OPAQUE
                        startDragAndDrop(dummyClip, shadow, ship, flags)
                        dragStarted = true
                        return true
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                Log.d(TAG, "ACTION_UP/CANCEL, selectedShip=${selectedShip}")
                selectedShip = null
                dragStarted = false
            }
        }

        // 7) Для Accessibility: если было ACTION_UP, вызываем performClick()
        if (event.actionMasked == MotionEvent.ACTION_UP) performClick()
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        // здесь обычно запускают логику клика, если он нужен
        return true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // Учтём padding, а также зарезервируем место под метки (ряд + столбец по 1 клетке)
        val horizontalPadding = paddingLeft + paddingRight
        val verticalPadding = paddingTop + paddingBottom

        // Доступная область для 10×10 сетки
        val availW = w - horizontalPadding
        val availH = h - verticalPadding

        // Размер клетки = min(ширина/11, высота/11), т.к. +1 клетка в каждой стороне на метки
        cellSize = min(availW / (GRID_SIZE + 1f), availH / (GRID_SIZE + 1f))
        gridWidth = cellSize * GRID_SIZE
        gridHeight = cellSize * GRID_SIZE

        // Смещение (левый верхний угол сетки): учитываем 1 клетку на метки
        offsetX = paddingLeft + cellSize
        offsetY = paddingTop + cellSize

        // Текстовый размер для меток = половина клетки
        textPaint.textSize = cellSize * 0.5f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawBackground(canvas)
        drawGrid(canvas)
        drawLabels(canvas)

        // 1) Сначала рисуем «hover» (если есть). Должно быть до drawShips(), чтобы фон был под спрайтами.
        hoverCell?.let { (r, c) ->
            val left = offsetX + c * cellSize
            val top = offsetY + r * cellSize
            canvas.drawRect(left, top, left + cellSize, top + cellSize, hoverPaint)
        }

        drawShips(canvas)
    }

    /** Рисует фон под сеткой */
    private fun drawBackground(canvas: Canvas) {
        canvas.drawRect(
            offsetX, offsetY,
            offsetX + gridWidth, offsetY + gridHeight,
            backgroundPaint
        )
    }

    /** Рисует вертикальные и горизонтальные линии сетки 10×10 */
    private fun drawGrid(canvas: Canvas) {
        for (i in 0..GRID_SIZE) {
            // Вертикальные линии
            val x = offsetX + i * cellSize
            canvas.drawLine(
                x, offsetY,
                x, offsetY + gridHeight,
                gridPaint
            )
            // Горизонтальные линии
            val y = offsetY + i * cellSize
            canvas.drawLine(offsetX, y,
                offsetX + gridWidth, y,
                gridPaint
            )
        }
    }

    /** Рисует метки столбцов (1–10) сверху и строк (A–J) слева */
    private fun drawLabels(canvas: Canvas) {
        for (i in 0 until GRID_SIZE) {
            // Цифры сверху
            val cx = offsetX + (i + 0.5f) * cellSize
            val cy = offsetY - cellSize / 3
            canvas.drawText("${i + 1}", cx, cy, textPaint)

            // Буквы слева
            val lx = offsetX - cellSize / 2
            val ly = offsetY + (i + 0.7f) * cellSize
            canvas.drawText(('A' + i).toString(), lx, ly, textPaint)
        }
    }

    /**
     * Рисует каждый корабль из [placements]:
     * – Если ship.fromTemplate == true – не рисуем, это «шаблон» на станции.
     * – Иначе: рисуем bitmap корабля, учитывая ориентацию (h/v).
     *   – Если ship.isInvalid == true – рисуем с красным полупрозрачным фоном и scale 0.9.
     */
    private fun drawShips(canvas: Canvas) {
        placements.forEach { ship ->
            if (ship.fromTemplate) return@forEach

            canvas.save()
            // Определяем, трясётся ли этот корабль: сравниваем ship.shipId
            val isShaking = (ship.shipId == shakingShipId)
            if (isShaking) {
                canvas.translate(shakeOffset, 0f)
            }

            val r0 = ship.startRow
            val c0 = ship.startCol
            val width = if (ship.isVertical) cellSize else ship.length * cellSize
            val height = if (ship.isVertical) ship.length * cellSize else cellSize
            val left = offsetX + c0 * cellSize
            val top = offsetY + r0 * cellSize


            if (ship.isInvalid) {
                // Рисуем красный фон именно под палубы клеток (invalidCellPaint)
                val cells = getOccupiedCells(ship)
                cells.forEach { (r, c) ->
                    val cl = offsetX + c * cellSize
                    val ct = offsetY + r * cellSize
                    canvas.drawRect(cl, ct, cl + cellSize, ct + cellSize, invalidCellPaint)
                }
                // Уменьшаем scale централизованно
                canvas.save()
                val centerX = left + width / 2
                val centerY = top + height / 2
                canvas.scale(INVALID_SCALE, INVALID_SCALE, centerX, centerY)
            }

            val key = "${ship.length}_${if (ship.isVertical) "v" else "h"}"
            val bitmap = shipBitmaps[key] ?: return@forEach
            val dest = RectF(left, top, left + width, top + height)
            canvas.drawBitmap(bitmap, null, dest, null)

            canvas.restore()
        }
    }

    /**
     * Ищет корабль (из [placements]), в котором содержатся координаты (x,y) на экране.
     * Возвращает ShipPlacementUi или null.
     * Нужно для обработки double-tap: понять, по какой модели тапнули.
     */
    private fun findShipAt(x: Float, y: Float): ShipPlacementUi? {
        placements.forEach { ship ->
            if (ship.fromTemplate) return@forEach
            val left = offsetX + ship.startCol * cellSize
            val top = offsetY + ship.startRow * cellSize
            val width = if (ship.isVertical) cellSize else ship.length * cellSize
            val height = if (ship.isVertical) ship.length * cellSize else cellSize
            val rect = RectF(left, top, left + width, top + height)
            if (rect.contains(x, y)) {
                return ship
            }
        }
        return null
    }

    private fun getOccupiedCells(ship: ShipPlacementUi): List<Pair<Int, Int>> {
        val list = mutableListOf<Pair<Int, Int>>()
        val r0 = ship.startRow
        val c0 = ship.startCol
        if (ship.isVertical) {
            for (i in 0 until ship.length) {
                list.add(r0 + i to c0)
            }
        } else {
            for (i in 0 until ship.length) {
                list.add(r0 to c0 + i)
            }
        }
        return list
    }

    /** Возвращает cellSize, чтобы Activity могла подогнать размеры шаблонов справа */
    fun getCellSize(): Float = cellSize

    /**
     * Проверяет, находится ли по крайней мере один корабль в списке [placements] на поле.
     * Нужен, чтобы проверить, можно ли «Очистить поле»: если всё пусто, послать подсказку.
     */
    fun isEmpty(): Boolean {
        return placements.none { !it.fromTemplate }
    }
}
