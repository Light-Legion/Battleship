package com.example.battleship_game.presentation.placement.manual

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipDescription
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.widget.TextView
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import com.example.battleship_game.R
import com.example.battleship_game.common.BaseActivity
import com.example.battleship_game.data.model.Difficulty
import com.example.battleship_game.data.model.ShipPlacementUi
import com.example.battleship_game.databinding.ActivityManualPlacementBinding
import com.example.battleship_game.dialog.CustomAlertDialog
import com.example.battleship_game.presentation.loading.LoadingActivity
import com.example.battleship_game.presentation.placement.auto.AutoPlacementActivity
import com.example.battleship_game.presentation.placement.save.SavePlacementActivity
import com.example.battleship_game.ui.ManualPlacementFieldView
import com.example.battleship_game.ui.ShipDragShadowBuilder
import com.google.android.material.snackbar.Snackbar

/**
 * Activity для экрана «Ручная расстановка» кораблей.
 *
 * 1) С правой стороны «станция» – ConstraintLayout с 10 TextView-ами (шаблоны кораблей).
 *    Пользователь может нажать на любой шаблон («тап») и «потянуть» его на игровое поле (drag-and-drop).
 *    После старта перетаскивания TextView становится INVISIBLE, а при возврате (если дроп неверный)
 *    – снова видимым.
 * 2) С левой стороны – Custom View [ManualPlacementFieldView], в который можно «бросить» корабль:
 *    – если дроп в пределах сетки, Activity получает [onShipDropped(ship, row, col)], пытается placeShip(...)
 *      через ViewModel и, в зависимости от результата, обновляет UI (FieldView + шаблоны).
 *    – если дроп за пределами поля (или палубная коллизия) → возвращаем TextView в первоначальное положение.
 * 3) Поддержка двойного тапа на уже размещённый корабль: Activity получает [onShipDoubleTapped(ship)] →
 *    вызывает rotateShip(...) → в зависимости от результата:
 *      – 0 (успех без пометок) – обновляем UI.
 *      – 1 (успех, но буферная коллизия) – обновляем UI, FieldView нарисует isInvalid=true.
 *      – 2 (провал из-за палубной коллизии) – вызываем animateShake(...) у FieldView и ничего не меняем.
 * 4) Кнопка «Очистить» сбрасывает все корабли на станцию.
 * 5) Кнопки «Сохранить» и «В бой!» активны только когда ViewModel.isPlacementValid == true.
 * 6) Если Activity запущена с fieldID >= 0 (из экрана загрузки сохранённой расстановки),
 *    сразу загружаем из БД (ViewModel.loadSavedPlacement()) и AI расставляет корабли автоматически.
 */
class ManualPlacementActivity : BaseActivity() {

    private lateinit var binding: ActivityManualPlacementBinding
    private val viewModel: ManualPlacementViewModel by viewModels()

    companion object {
        const val EXTRA_FIELD_ID = "EXTRA_FIELD_ID"
        const val EXTRA_DIFFICULTY = "EXTRA_DIFFICULTY"
    }

    /** Чтобы сопоставлять shipId → station TextView */
    private val stationMap: MutableMap<Int, TextView> = mutableMapOf()

    /**
     * Храним старые координаты (row, col) корабля с поля,
     * чтобы при «палубной коллизии» вернуть именно туда.
     */
    private val oldPositionMap: MutableMap<Int, Pair<Int, Int>> = mutableMapOf()

    private val savePlacementLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // Показываем сообщение, только если расстановка была сохранена
            Snackbar.make(binding.main, R.string.hint_save_placement, Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityManualPlacementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyEdgeInsets(binding.main)
        enterImmersiveMode()

        // 1) Читаем fieldID и difficulty из Intent-а
        parseIntentExtras()

        // 2) Подписываемся на LiveData из ViewModel:
        observeViewModel()

        // 3) Инициализируем станцию кораблей (TextView-ы) и навешиваем drag-нулисенеры
        setupStationShips()

        // 4) Настраиваем ManualPlacementFieldView: слушатели drop и double-tap
        setupFieldViewListeners()

        // 5) Настраиваем остальные кнопки
        setupButtons()

        binding.bfv.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                adjustShipTemplates()
                binding.bfv.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })

        // Если пришли из «Загрузки сохранённой» (fieldID >= 0), загружаем из БД
        viewModel.loadSavedPlacement()

        onBackPressedDispatcher.addCallback(this) {
            finish()
        }
    }

    /** Считывает fieldID и difficulty из Intent-а */
    private fun parseIntentExtras() {
        viewModel.fieldID = intent
            .getLongExtra(EXTRA_FIELD_ID, -1L)

        // Получаем сложность из настроек и сохраняем в viewModel
        viewModel.difficulty = intent
            .getSerializableExtra(EXTRA_DIFFICULTY) as? Difficulty ?: Difficulty.MEDIUM
    }

    /** Подписываемся на LiveData из ViewModel */
    private fun observeViewModel() {
        // Когда обновились шаблоны → обновляем видимость TextView на «станции»
        viewModel.templates.observe(this, Observer { templates ->
            // Сначала скрываем все TextView станции
            stationMap.values.forEach { it.isVisible = false }
            // Для каждого шаблона из списка делаем его VISIBLE (по shipId ищем TextView)
            templates.forEach { shipUi ->
                stationMap[shipUi.shipId]?.isVisible = true
            }
        })

        // Когда обновились размещённые корабли → передаём их в FieldView
        viewModel.currentPlacements.observe(this, Observer { placedList ->
            binding.bfv.updateShips(placedList)
        })

        // Когда пересчитывается валидность → активируем/деактивируем кнопки
        viewModel.isPlacementValid.observe(this, Observer { valid ->
            binding.btnToBattle.isEnabled = valid
        })
    }

    /** Привязываем все 10 TextView-шаблонов справа и вешаем на них drag-лисенеры */
    @SuppressLint("ClickableViewAccessibility")
    private fun setupStationShips() {
        // Сначала найдём все TextView по их ID и запомним в stationMap: shipId → TextView
        stationMap.clear()
        stationMap[1] = binding.tvShipTemplate4
        stationMap[2] = binding.tvShipTemplate31
        stationMap[3] = binding.tvShipTemplate32
        stationMap[4] = binding.tvShipTemplate21
        stationMap[5] = binding.tvShipTemplate22
        stationMap[6] = binding.tvShipTemplate23
        stationMap[7] = binding.tvShipTemplate11
        stationMap[8] = binding.tvShipTemplate12
        stationMap[9] = binding.tvShipTemplate13
        stationMap[10] = binding.tvShipTemplate14

        // Для каждой TextView навесим OnTouchListener для старта drag-and-drop
        stationMap.forEach { (shipId, tv) ->
            tv.setOnTouchListener { view, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    // При первом касании создаём ClipData с простым MIME (TEXT_PLAIN)
                    val item = ClipData.Item(shipId.toString())
                    val dragData = ClipData(
                        shipId.toString(),
                        arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN),
                        item
                    )

                    // 2) Вычисляем dx, dy (смещение тени), чтобы палец был над первой палубой
                    val length = shipIdLength(shipId) // 4, 3, 2 или 1
                    val cellSize = binding.bfv.getCellSize()
                    val shadowWidth = (length * cellSize).toInt()
                    val shadowHeight = cellSize.toInt()
                    // touchX, touchY — точка касания внутри тени
                    val touchX = (cellSize / 2).toInt()
                    val touchY = (cellSize / 2).toInt()

                    // 3) Получаем bitmap для горизонтального корабля‐шаблона
                    val context = view.context
                    val options = BitmapFactory.Options().apply {
                        inScaled = true
                        inDensity = DisplayMetrics.DENSITY_DEFAULT
                        inTargetDensity = context.resources.displayMetrics.densityDpi
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }
                    val shipBitmap = when (length) {
                        4 -> BitmapFactory.decodeResource(context.resources, R.drawable.ship_horizontal_4, options)
                        3 -> BitmapFactory.decodeResource(context.resources, R.drawable.ship_horizontal_3, options)
                        2 -> BitmapFactory.decodeResource(context.resources, R.drawable.ship_horizontal_2, options)
                        else -> BitmapFactory.decodeResource(context.resources, R.drawable.ship_horizontal_1, options)
                    }

                    // 4) Создаём ShipDragShadowBuilder
                    val shadow = ShipDragShadowBuilder(
                        shipBitmap,
                        shadowWidth,
                        shadowHeight,
                        touchX,
                        touchY
                    )

                    // 5) Передаём смещение тени в FieldView, чтобы она правильно рисовала hover:
                    binding.bfv.setDragShadowOffset(touchX.toFloat(), touchY.toFloat())

                    // 6) В localState передаём объект ShipPlacementUi (шаблон):
                    val template = viewModel.templates.value
                        ?.firstOrNull { it.shipId == shipId }
                    if (template != null) {
                        view.startDragAndDrop(
                            dragData,
                            shadow,
                            template,  // localState = ShipPlacementUi
                            0
                        )
                        view.visibility = View.INVISIBLE
                        return@setOnTouchListener true
                    }
                }
                return@setOnTouchListener false
            }
        }
    }

    /**
     * Принимает длину корабля по shipId (1→4, 2-3→3, 4-6→2, 7-10→1).
     * Нужна, чтобы вычислить dx (смещение тени по X).
     */
    private fun shipIdLength(shipId: Int): Int {
        return when (shipId) {
            1 -> 4
            2, 3 -> 3
            in 4..6 -> 2
            in 7..10 -> 1
            else -> 1
        }
    }

    /** Настраиваем слушатели [ManualPlacementFieldView] */
    private fun setupFieldViewListeners() {
        // 1) При дропе на поле
        binding.bfv.setOnShipDropListener(object : ManualPlacementFieldView.OnShipDropListener {
            override fun onShipDropped(ship: ShipPlacementUi, row: Int, col: Int) {
                // Вызываем ViewModel.placeShip(...) и обрабатываем результат
                when (viewModel.placeShip(ship, row, col)) {
                    0 -> {
                        // УСПЕШНО без пометок → уже обновлено через LiveData
                        // Ничего больше делать не нужно
                    }
                    1 -> {
                        // УСПЕШНО, но «буферная» коллизия (ship.isInvalid=true) → LiveData обновилось
                        // Ничего дополнительно делать не нужно: FieldView перерисует красным/уменьшенным
                    }
                    2 -> {
                        val id = ship.shipId
                        val oldPos = oldPositionMap[id]
                        if (oldPos != null) {
                            viewModel.returnShipToField(
                                ship,
                                oldRow = oldPos.first,
                                oldCol = oldPos.second
                            )
                        } else {
                            // если вдруг нет старой позиции, считаем, что это «возврат на станцию»
                            viewModel.returnShipToStation(ship)
                            stationMap[ship.shipId]?.visibility = View.VISIBLE
                        }
                    }
                }
            }

            override fun onShipDroppedOutside(ship: ShipPlacementUi) {
                // Если дропнули за пределы поля → сразу возвращаем на станцию
                viewModel.returnShipToStation(ship)
                stationMap[ship.shipId]?.visibility = View.VISIBLE
            }
        })

        // 2) При двойном тапе
        binding.bfv.setOnShipDoubleTapListener(object : ManualPlacementFieldView.OnShipDoubleTapListener {
            override fun onShipDoubleTapped(ship: ShipPlacementUi) {
                when (viewModel.rotateShip(ship)) {
                    0 -> {
                        // УСПЕШНО без пометок → LiveData обновило FieldView
                    }
                    1 -> {
                        // УСПЕШНО, но «буферная» коллизия → LiveData обновило FieldView,
                        // ship.isInvalid=true → FieldView отрисует красным и уменьшит scale
                    }
                    2 -> {
                        // НЕУСПЕШНО (палубная коллизия) → анимируем «дрожание»
                        binding.bfv.animateShipShake(ship)
                    }
                }
            }
        })

        // 3) PickFromField (ACTION_MOVE) → удалить с поля, но **не** возвращать в templates
        binding.bfv.onShipPickedFromField = { ship ->
            oldPositionMap[ship.shipId] = Pair(ship.startRow, ship.startCol)
            viewModel.pickShipFromField(ship)
        }
    }

    /** Настройка кнопок «Очистить», «Сохранить» и «В бой!» */
    private fun setupButtons() {
        binding.apply {
            // a) Кнопка «Назад»
            btnBack.setOnClickListener { finish() }

            // b) Кнопка «Очистить»
            btnClear.setOnClickListener {
                if (binding.bfv.isEmpty()) {
                    // Если поле и так пустое → подсказка
                    Snackbar.make(main, R.string.hint_clear_already, Snackbar.LENGTH_SHORT).show()
                } else {
                    // Сбрасываем во ViewModel
                    viewModel.clearPlacement()
                    // Все шаблоны вновь станут visible через observer templates
                }
            }

            // c) Кнопка «Сохранить»
            btnSave.setOnClickListener {
                // Берём все корабли, стоящие на поле (fromTemplate=false, isInvalid=false)
                val placedValid = viewModel.currentPlacements.value
                    ?.filter { !it.fromTemplate && !it.isInvalid }
                    ?: emptyList()
                if (placedValid.size == 10) {
                    // После подтверждения переходим на SavePlacementActivity
                    val busList = placedValid.map { it.base }
                    savePlacementLauncher.launch(
                        Intent(
                            this@ManualPlacementActivity,
                            SavePlacementActivity::class.java
                        )
                            .putParcelableArrayListExtra(
                                SavePlacementActivity.EXTRA_SHIPS,
                                ArrayList(busList)
                            )
                    )
                } else {
                    // Открываем диалог предупреждения, если хотим проверить ещё раз
                    showExitConfirmDialog()
                }
            }

            // d) Кнопка «В бой!»
            btnToBattle.setOnClickListener {
                // Аналогично – все 10 валидных кораблей
                val placedValid = viewModel.currentPlacements.value
                    ?.filter { !it.fromTemplate && !it.isInvalid }
                    ?: emptyList()
                if (placedValid.size == 10) {
                    val busList = placedValid.map { it.base }
                    startActivity(
                        Intent(this@ManualPlacementActivity, LoadingActivity::class.java)
                            .putParcelableArrayListExtra(
                                LoadingActivity.EXTRA_PLAYER_SHIPS,
                                ArrayList(busList)
                            )
                            .putExtra(EXTRA_DIFFICULTY, viewModel.difficulty)
                    )
                } else {
                    Snackbar.make(main, R.string.hint_battle_invalid, Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Динамически масштабирует все TextView-шаблоны кораблей,
     * чтобы их ширина = length * cellSize, а высота = cellSize.
     */
    private fun adjustShipTemplates() {
        // 1) Получаем текущее значение cellSize из ManualPlacementFieldView (bfv)
        val cellSizePx: Float = binding.bfv.getCellSize()

        // 2) Для каждого TextView-шаблона устанавливаем нужные размеры
        //    Шаблоны находятся внутри layoutShips (ConstraintLayout)
        with(binding) {
            val rows = listOf(
                listOf(tvShipTemplate4),
                listOf(tvShipTemplate31, tvShipTemplate32),
                listOf(tvShipTemplate21, tvShipTemplate22, tvShipTemplate23),
                listOf(tvShipTemplate11, tvShipTemplate12, tvShipTemplate13, tvShipTemplate14)
            )

            var currentPosition = 0
            val shipHeight = cellSizePx.toInt()
            val rowMargin = (24 * resources.displayMetrics.density).toInt()
            rows.forEachIndexed { index, ships ->
                val length = when(index) {
                    0 -> 4   // Первая строка: 4-палубные
                    1 -> 3   // Вторая строка: 3-палубные
                    2 -> 2   // Третья строка: 2-палубные
                    else -> 1 // Четвертая строка: 1-палубные
                }

                ships.forEach { ship ->
                    resizeTemplate(ship, length, cellSizePx)
                }

                if (index < rows.size - 1) {
                    currentPosition += shipHeight + rowMargin
                    when (index) {
                        0 -> guideline1.setGuidelineBegin(currentPosition)
                        1 -> guideline2.setGuidelineBegin(currentPosition)
                        2 -> guideline3.setGuidelineBegin(currentPosition)
                    }
                }
            }
        }
    }

    /**
     * Устанавливает TextView-шаблону корабля нужные размеры:
     *   width  = length * cellSizePx,
     *   height = cellSizePx.
     *
     * @param tv       TextView-шаблон из layoutShips
     * @param length   Длина корабля (1,2,3 или 4)
     * @param cellSizePx Размер одной клетки (в пикселях)
     */
    private fun resizeTemplate(tv: TextView, length: Int, cellSizePx: Float) {
        val params = tv.layoutParams
        // width = длина корабля * размер клетки
        params.width = (length * cellSizePx).toInt()
        // height = размер клетки
        params.height = cellSizePx.toInt()
        tv.layoutParams = params
    }

    private fun showExitConfirmDialog() {
        CustomAlertDialog(this)
            .setIcon(R.drawable.ic_dialog_warning)
            .setTitle(R.string.error_save_title)
            .setMessage(R.string.error_save_manual_placement_message)
            .setPositiveButtonText(R.string.action_ok)
            .show()
    }
}