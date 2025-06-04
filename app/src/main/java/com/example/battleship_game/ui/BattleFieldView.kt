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
import android.view.MotionEvent
import android.view.View
import com.example.battleship_game.R
import com.example.battleship_game.data.model.ShipPlacement
import kotlin.collections.forEach
import kotlin.math.min
import androidx.core.graphics.scale
import com.example.battleship_game.strategies.shooting.CellState

/**
 * BattleFieldView — кастомный View для отображения 10×10 поля «Морской бой».
 *
 * Основные функции:
 *  1) Центрирование поля внутри контейнера (с учётом места под метки).
 *  2) Отрисовка сетки 10×10 с буквенными/цифровыми метками.
 *  3) Хранение и отображение состояний клеток: EMPTY, SHIP, HIT, MISS, SUNK.
 *  4) Отображение кораблей (для поля игрока) и скрытие их (для поля компьютера) до полного потопления.
 *  5) Обработка тапов по клеткам с коллбэком (row, col) для поля компьютера.
 *  6) Пометка атакуемых клеток (hit/miss) и потопленных кораблей с буферной зоной.
 *  7) Отрисовка потопленных кораблей «под» крестиками попаданий.
 *
 * Использование:
 *  - Установить fieldType = FieldType.PLAYER или FieldType.COMPUTER.
 *  - Вызвать setPlacements(listOfShipPlacements).
 *  - Если поле компьютера, назначить cellClickListener.
 *  - При выстреле вызывать markHit(...) или markMiss(...).
 *  - При потоплении вызывать markSunkShip(..., bufferCells).
 */
class BattleFieldView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // ------------------------------------------------------------
    // 1. Конфигурационные перечисления
    // ------------------------------------------------------------

    /** Тип поля: PLAYER — рисовать свои корабли; COMPUTER — скрывать чужие */
    enum class FieldType { PLAYER, COMPUTER }

    // ------------------------------------------------------------
    // 2. Публичные свойства
    // ------------------------------------------------------------

    /** Текущий режим поля: PLAYER (рисовать корабли) или COMPUTER (скрывать) */
    var fieldType: FieldType = FieldType.PLAYER
        set(value) {
            field = value
            invalidate() // при смене поля перерисуем всё заново
        }

    /** Коллбэк — вызывается, когда пользователь тапает по (row, col) */
    var cellClickListener: ((row: Int, col: Int) -> Unit)? = null

    // ------------------------------------------------------------
    // 3. Локальные поля для отрисовки
    // ------------------------------------------------------------

    private var cellSize = 0f           // Размер одной клетки (в пикселях)
    private var gridWidth = 0f          // Ширина всей сетки (10 * cellSize)
    private var gridHeight = 0f         // Высота всей сетки (10 * cellSize)
    private var offsetX = 0f            // Смещение по X к левому краю сетки
    private var offsetY = 0f            // Смещение по Y к верхнему краю сетки
    private var labelTextSize = 0f      // Размер шрифта для меток (рассчитывается в onSizeChanged)
    private var reserveLeft = 0f        // «Запас» слева под буквы (A–J)
    private var reserveTop = 0f         // «Запас» сверху под цифры (1–10)

    // ------------------------------------------------------------
    // 4. Paint, Bitmap и ресурсы
    // ------------------------------------------------------------

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.BLACK
        strokeWidth = resources.displayMetrics.density * 1.5f
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textAlign = Paint.Align.CENTER
    }

    private val shipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isDither = true
        isFilterBitmap = true
    }

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.game_field) // слегка голубоватый полупрозрачный фон
        style = Paint.Style.FILL
    }

    private var hitBitmap: Bitmap
    private var missBitmap: Bitmap

    // Кэш всех битмапов для кораблей, ключ = "{length}_{orientation}", например "3_h" или "4_v"
    private val shipBitmaps = mutableMapOf<String, Bitmap>()

    // ------------------------------------------------------------
    // 5. Внутреннее состояние поля
    // ------------------------------------------------------------

    /** Список всех кораблей на поле (только для PLAYER). Каждый объект содержит (length, startRow, startCol, isVertical). */
    private var placements: List<ShipPlacement> = emptyList()

    /**
     * Матрица 10×10 хранящая текущее состояние каждой клетки:
     *   EMPTY, SHIP, HIT, MISS, SUNK.
     */
    private val cellStates = Array(10) { Array(10) { CellState.EMPTY } }

    /**
     * Набор потопленных кораблей. Нужен, чтобы в drawSunkShips
     * правильно отрисовать затопленный корабль «под» крестиками.
     */
    private val sunkShips = mutableSetOf<ShipPlacement>()

    // ------------------------------------------------------------
    // 6. Инициализация: загрузка ресурсов
    // ------------------------------------------------------------

    init {
        val options = BitmapFactory.Options().apply {
            inScaled = true
            inDensity = DisplayMetrics.DENSITY_DEFAULT
            inTargetDensity = resources.displayMetrics.densityDpi
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        // Загрузка растровых ресурсов
        hitBitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_ship_hit, options)
        missBitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_shot_miss, options)

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

    // ------------------------------------------------------------
    // 7. Замена клеточного состояния и помощь в сбросе
    // ------------------------------------------------------------

    /**
     * Сбрасывает всю матрицу cellStates: сначала в EMPTY, затем
     * помечает клетки под кораблями как CellState.SHIP.
     */
    private fun resetCellStates() {
        // Все в EMPTY
        for (r in 0 until 10) {
            for (c in 0 until 10) {
                cellStates[r][c] = CellState.EMPTY
            }
        }
        sunkShips.clear()

        // Пометка SHIP (только для PLAYER-поля, но мы сохраняем сюда для упрощения)
        placements.forEach { ship ->
            for (i in 0 until ship.length) {
                val row = ship.startRow + if (ship.isVertical) i else 0
                val col = ship.startCol + if (ship.isVertical) 0 else i
                if (row in 0..9 && col in 0..9) {
                    cellStates[row][col] = CellState.SHIP
                }
            }
        }
    }

    /**
     * Проверка: вся ли палуба корабля потоплена? (т. е. все соответствующие cellStates == SUNK)
     */
    private fun shipIsFullySunk(ship: ShipPlacement): Boolean {
        for (i in 0 until ship.length) {
            val row = ship.startRow + if (ship.isVertical) i else 0
            val col = ship.startCol + if (ship.isVertical) 0 else i
            if (cellStates[row][col] != CellState.SUNK) {
                return false
            }
        }
        return true
    }

    // ------------------------------------------------------------
    // 8. Публичные методы для изменения состояния из Activity/ViewModel
    // ------------------------------------------------------------

    /**
     * Устанавливает новый список размещений кораблей.
     * Автоматически сбрасывает cellStates под новые корабли.
     */
    fun setPlacements(list: List<ShipPlacement>) {
        placements = list
        resetCellStates()
        invalidate()
    }

    /**
     * Проверка, была ли уже стрельба по клетке (row, col).
     * @return true, если состояние != EMPTY/SHIP.
     */
    fun isCellShot(row: Int, col: Int): Boolean {
        return cellStates[row][col] == CellState.HIT
                || cellStates[row][col] == CellState.MISS
                || cellStates[row][col] == CellState.SUNK
    }

    /**
     * Пометить клетку (row,col) как попадание (HIT).
     */
    fun markHit(row: Int, col: Int) {
        if (row !in 0..9 || col !in 0..9) return
        cellStates[row][col] = CellState.HIT
        invalidateCell(row, col)
    }

    /**
     * Пометить клетку (row,col) как промах (MISS).
     */
    fun markMiss(row: Int, col: Int) {
        if (row !in 0..9 || col !in 0..9) return
        cellStates[row][col] = CellState.MISS
        invalidateCell(row, col)
    }

    /**
     * Пометить весь корабль как потопленный (SUNK).
     * При этом автоматически заполняет буферную зону (окрестные клетки) как MISS.
     *
     * @param ship Потопленный ShipPlacement.
     * @param bufferCells Список координат (row,col) буферной зоны вокруг корабля.
     */
    fun markSunkShip(ship: ShipPlacement, bufferCells: List<Pair<Int, Int>>) {
        // Сначала помечаем палубы корабля как SUNK
        for (i in 0 until ship.length) {
            val r = ship.startRow + if (ship.isVertical) i else 0
            val c = ship.startCol + if (ship.isVertical) 0 else i
            if (r in 0..9 && c in 0..9) {
                cellStates[r][c] = CellState.SUNK
            }
        }
        sunkShips.add(ship)

        // Помечаем буферные клетки как промах (если они были EMPTY или SHIP)
        bufferCells.forEach { (r, c) ->
            if (r in 0..9 && c in 0..9 &&
                (cellStates[r][c] == CellState.EMPTY || cellStates[r][c] == CellState.SHIP)
            ) {
                cellStates[r][c] = CellState.MISS
            }
        }

        // Перерисовываем всё целиком (можно оптимизировать до отдельных invalidateCell)
        invalidate()
    }

    /**
     * Сбрасывает всю историю выстрелов и возвращает состояние клеток к первичному (только SHIP/EMPTY).
     */
    fun resetShotsAndShips() {
        resetCellStates()
        invalidate()
    }

    /**
     * Устанавливает коллбэк, который будет вызван, когда пользователь тапает по клетке (row, col).
     */
    fun setOnCellClickListener(listener: (Int, Int) -> Unit) {
        cellClickListener = listener
    }

    // ------------------------------------------------------------
    // 9. Определение размеров и масштабирование битмапов
    // ------------------------------------------------------------

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // 1) Считаем «сырой» размер клетки без учёта меток:
        val rawCellW = (w - paddingLeft - paddingRight) / 10f
        val rawCellH = (h - paddingTop - paddingBottom) / 10f
        val unreservedCell = min(rawCellW, rawCellH)

        // 2) Решаем, сколько места оставить под метки (буквы слева и цифры сверху).
        //    Например, резервируем ≈ 0.8*unreservedCell, чтобы метки не накладывались.
        reserveLeft = unreservedCell * 0.8f
        reserveTop = unreservedCell * 0.8f

        // 3) Доступное пространство под 10×10 сетку (с учётом reserveLeft/reserveTop).
        val availW = w - paddingLeft - paddingRight - reserveLeft
        val availH = h - paddingTop - paddingBottom - reserveTop

        // 4) Окончательный размер одной клетки
        cellSize = min(availW / 10f, availH / 10f)
        gridWidth = cellSize * 10f
        gridHeight = cellSize * 10f

        // 5) Центрируем сетку внутри свободного пространства, оставляя место для меток
        offsetX = paddingLeft + reserveLeft + (availW - gridWidth) / 2f
        offsetY = paddingTop + reserveTop + (availH - gridHeight) / 2f

        // 6) Размер шрифта для меток ≈ 50% от cellSize
        labelTextSize = cellSize * 0.5f
        labelPaint.textSize = labelTextSize

        // 7) Для крестика/промаха: масштабируем до ~80% cellSize, чтобы они центровались
        hitBitmap = hitBitmap.scaledCellBitmap(cellSize)
        missBitmap = missBitmap.scaledCellBitmap(cellSize)
    }

    /**
     * Возвращает новый Bitmap, масштабированный под ~80% от cellSize (центруем).
     */
    private fun Bitmap.scaledCellBitmap(cellSize: Float): Bitmap {
        val target = (cellSize * 0.8f).toInt().coerceAtLeast(1)
        return if (width != target || height != target) {
            this.scale(target, target)
        } else {
            this
        }
    }

    // ------------------------------------------------------------
    // 10. Рисующие методы
    // ------------------------------------------------------------

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 1) Полупрозрачный фон за сеткой
        drawBackground(canvas)

        // 2) Сетка и метки
        drawGrid(canvas)
        drawLabels(canvas)

        // 3) Если это поле игрока — рисуем ВСЕ корабли
        if (fieldType == FieldType.PLAYER) {
            drawShips(canvas)
        } else {
            // Если это поле компьютера — рисуем лишь потопленные корабли
            drawSunkShips(canvas)
        }

        // 4) Поверх всего — рисуем крестики (HIT) и штриховку (MISS)
        drawShots(canvas)
    }

    /** Рисует полупрозрачный фон под сеткой */
    private fun drawBackground(canvas: Canvas) {
        canvas.drawRect(
            offsetX, offsetY,
            offsetX + gridWidth, offsetY + gridHeight,
            backgroundPaint
        )
    }

    /** Рисует линии сетки 10×10 */
    private fun drawGrid(canvas: Canvas) {
        for (i in 0..10) {
            // Вертикальные линии
            val x = offsetX + i * cellSize
            canvas.drawLine(x, offsetY, x, offsetY + gridHeight, gridPaint)

            // Горизонтальные линии
            val y = offsetY + i * cellSize
            canvas.drawLine(offsetX, y, offsetX + gridWidth, y, gridPaint)
        }
    }

    /** Рисует метки: цифры сверху (1…10) и буквы слева (A…J) */
    private fun drawLabels(canvas: Canvas) {
        // Цифры (1…10) сверху поля:
        for (i in 0 until 10) {
            val cx = offsetX + (i + 0.5f) * cellSize
            // Поднимаем на 0.2 от cellSize от верхней линии поля
            val cy = offsetY - cellSize * 0.3f
            canvas.drawText("${i + 1}", cx, cy, labelPaint)
        }
        // Буквы (A…J) слева от поля:
        for (i in 0 until 10) {
            val cx = offsetX - cellSize * 0.5f
            val cy = offsetY + (i + 0.7f) * cellSize
            canvas.drawText(('A' + i).toString(), cx, cy, labelPaint)
        }
    }

    /** Отрисовывает все корабли (только для PLAYER). */
    private fun drawShips(canvas: Canvas) {
        placements.forEach { ship ->
            val key = "${ship.length}_${if (ship.isVertical) "v" else "h"}"
            val origBmp = shipBitmaps[key] ?: return@forEach

            // Расчет позиции и размера прямоугольника
            val left = offsetX + ship.startCol * cellSize
            val top = offsetY + ship.startRow * cellSize
            val width  = if (ship.isVertical) cellSize else ship.length * cellSize
            val height = if (ship.isVertical) ship.length * cellSize else cellSize

            val destRect = RectF(left, top, left + width, top + height)
            // Рисуем оригинальный bitmap в destRect; Canvas сам сделает фильтрацию
            canvas.drawBitmap(origBmp, null, destRect, shipPaint)
        }
    }

    /**
     * Рисует корабли компьютера, но только те, у которых все палубы = SUNK.
     * Вызывается только когда fieldType == COMPUTER.
     */
    private fun drawSunkShips(canvas: Canvas) {
        sunkShips.forEach { ship ->
            // Если корабль компьютера ещё не полностью потоплен, пропускаем
            if (!shipIsFullySunk(ship)) return@forEach

            val key = "${ship.length}_${if (ship.isVertical) "v" else "h"}"
            val origBmp = shipBitmaps[key] ?: return@forEach

            val left = offsetX + ship.startCol * cellSize
            val top = offsetY + ship.startRow * cellSize
            val width  = if (ship.isVertical) cellSize else ship.length * cellSize
            val height = if (ship.isVertical) ship.length * cellSize else cellSize

            val destRect = RectF(left, top, left + width, top + height)
            canvas.drawBitmap(origBmp, null, destRect, shipPaint)
        }
    }

    /** Рисует крестики (HIT) и штриховку (MISS) поверх поля */
    private fun drawShots(canvas: Canvas) {
        for (r in 0 until 10) {
            for (c in 0 until 10) {
                when (cellStates[r][c]) {
                    CellState.HIT, CellState.SUNK -> {
                        // Центрируем крести в клетке
                        val cx = offsetX + (c + 0.5f) * cellSize
                        val cy = offsetY + (r + 0.5f) * cellSize
                        val halfW = hitBitmap.width / 2f
                        val halfH = hitBitmap.height / 2f
                        canvas.drawBitmap(hitBitmap, cx - halfW, cy - halfH, null)
                    }
                    CellState.MISS -> {
                        val cx = offsetX + (c + 0.5f) * cellSize
                        val cy = offsetY + (r + 0.5f) * cellSize
                        val halfW = missBitmap.width / 2f
                        val halfH = missBitmap.height / 2f
                        canvas.drawBitmap(missBitmap, cx - halfW, cy - halfH, null)
                    }
                    else -> {
                        // EMPTY или SHIP (для поля COMPUTER → рисуем SHIP как пустую клетку)
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------
    // 11. Обработка касаний
    // ------------------------------------------------------------

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (fieldType == FieldType.PLAYER) {
            // По своему полю не стреляют
            return false
        }
        if (event.action == MotionEvent.ACTION_DOWN) {
            val x = event.x
            val y = event.y
            if (x < offsetX || x >= offsetX + gridWidth ||
                y < offsetY || y >= offsetY + gridHeight
            ) {
                return false
            }
            val col = ((x - offsetX) / cellSize).toInt()
            val row = ((y - offsetY) / cellSize).toInt()
            if (row in 0..9 && col in 0..9) {
                // Если клетка уже обстреляна, ничего не делаем
                if (!isCellShot(row, col)) {
                    performClick()
                    cellClickListener?.invoke(row, col)
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        // Вызываем родительский performClick(), чтобы система зарегистрировала клик (Accessibility, фокус и звук)
        super.performClick()
        return true
    }

    // ------------------------------------------------------------
    // 12. Умная инвалидация одной клетки
    // ------------------------------------------------------------

    /**
     * Инвалидирует именно ту область, где лежит клетка (row, col), чтобы
     * не перерисовывать всё поле целиком, а только нужную ячейку.
     */
    private fun invalidateCell(row: Int, col: Int) {
        val left = (offsetX + col * cellSize).toInt()
        val top  = (offsetY + row * cellSize).toInt()
        val right = (left + cellSize).toInt()
        val bottom = (top + cellSize).toInt()

        postInvalidate(left, top, right, bottom)
    }

    // ------------------------------------------------------------
    // 13. Освобождение ресурсов
    // ------------------------------------------------------------

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        hitBitmap.recycle()
        missBitmap.recycle()
        shipBitmaps.values.forEach {
            if (!it.isRecycled) it.recycle()
        }
        shipBitmaps.clear()
    }
}