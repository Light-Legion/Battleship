package com.example.battleship_game.presentation.placement.manual

import android.content.Intent
import android.os.Bundle
import android.view.ViewTreeObserver
import android.widget.TextView
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import com.example.battleship_game.common.BaseActivity
import com.example.battleship_game.data.db.AppDatabase
import com.example.battleship_game.data.model.Difficulty
import com.example.battleship_game.data.model.ShipPlacement
import com.example.battleship_game.data.repository.PlacementRepository
import com.example.battleship_game.databinding.ActivityManualPlacementBinding
import com.example.battleship_game.presentation.loading.LoadingActivity
import com.example.battleship_game.presentation.placement.save.SavePlacementActivity

/**
 * Activity для ручной расстановки кораблей.
 *
 * Показывает:
 *  - [com.example.battleship_game.ui.ManualPlacementFieldView] слева,
 *  - шаблоны кораблей справа (1–2–3–4 в строках),
 *  - кнопки «Назад», «Сохранить», «Очистить», «В бой!».
 */
class ManualPlacementActivity : BaseActivity()
     // имплементируем коллбэки поля
{

    private lateinit var binding: ActivityManualPlacementBinding
    private val viewModel: ManualPlacementViewModel by viewModels()

    companion object {
        const val EXTRA_FIELD_ID = "EXTRA_FIELD_ID"
        const val EXTRA_DIFFICULTY = "EXTRA_DIFFICULTY"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityManualPlacementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyEdgeInsets(binding.main)
        enterImmersiveMode()

        parseIntentExtras()

        viewModel.loadSavedPlacement()
        observeSavedPlacements()

        setupUI()

        onBackPressedDispatcher.addCallback(this) {
            finish()
        }
    }

    private fun parseIntentExtras() {
        viewModel.fieldID = intent
            .getLongExtra(EXTRA_FIELD_ID, 0)

        // Получаем сложность из настроек и сохраняем в viewModel
        viewModel.difficulty = intent
            .getSerializableExtra(EXTRA_DIFFICULTY) as? Difficulty ?: Difficulty.MEDIUM
    }

    /**
     * Подписываемся на LiveData savedPlacements из ViewModel.
     * Как только придёт список (либо пустой), очищаем поле и, если список не пуст,
     * расставляем корабли по сохранённым координатам.
     */
    private fun observeSavedPlacements() {
        viewModel.savedPlacements.observe(this, Observer { shipList ->
            binding.bfv.setPlacements(shipList)
        })
    }

    private fun setupUI() {
        binding.apply {
            bfv.viewTreeObserver.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    // bfv (ManualPlacementFieldView) уже измерился, cellSize вычислено
                    adjustShipTemplates()

                    // Убираем listener, чтобы этот код не вызывался повторно
                    bfv.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            })

            btnBack.setOnClickListener {
                finish()
            }

            // BattleFieldView: слушаем события
            //bfv.listener = this@ManualPlacementActivity

            // Настраиваем RecyclerView шаблонов в GridLayoutManager(4) + SpanSizeLookup
            viewModel.templates.observe(this@ManualPlacementActivity) { templates ->
                val glm = GridLayoutManager(this@ManualPlacementActivity, 4)
                glm.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int {
                        return when (templates[position].length) {
                            4 -> 4 // одна в строке
                            3 -> 2 // две по 3-пал.
                            else -> 1 // по одной
                        }
                    }
                }
            }

            // Кнопка «Очистить поле»
            btnClear.setOnClickListener {
                bfv.setPlacements(emptyList())
            }

            // В бой!
            btnToBattle.setOnClickListener {
                val placed = emptyList<ShipPlacement>()
                // Передаём список в следующую Activity, например, в GameActivity
                startActivity(
                    Intent(this@ManualPlacementActivity, LoadingActivity::class.java).apply {
                        putParcelableArrayListExtra(LoadingActivity.EXTRA_PLAYER_SHIPS, ArrayList(placed))
                        putExtra(EXTRA_DIFFICULTY, viewModel.difficulty)
                    })
            }

            // Кнопка "Сохранить"
            btnSave.setOnClickListener {
                val placedShips: List<ShipPlacement> = binding.bfv.getCurrentPlacements()
                startActivity(
                    Intent(this@ManualPlacementActivity, SavePlacementActivity::class.java).apply {
                        putParcelableArrayListExtra(
                            SavePlacementActivity.EXTRA_SHIPS,
                            ArrayList(placedShips)
                        )
                    }
                )
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
            // Шаблон «4-палубный» (tvShipTemplate4)
            resizeTemplate(tvShipTemplate4, length = 4, cellSizePx)

            // Шаблоны «3-палубные»
            resizeTemplate(tvShipTemplate31, length = 3, cellSizePx)
            resizeTemplate(tvShipTemplate32, length = 3, cellSizePx)

            // Шаблоны «2-палубные»
            resizeTemplate(tvShipTemplate21, length = 2, cellSizePx)
            resizeTemplate(tvShipTemplate22, length = 2, cellSizePx)
            resizeTemplate(tvShipTemplate23, length = 2, cellSizePx)

            // Шаблоны «1-палубные»
            resizeTemplate(tvShipTemplate11, length = 1, cellSizePx)
            resizeTemplate(tvShipTemplate12, length = 1, cellSizePx)
            resizeTemplate(tvShipTemplate13, length = 1, cellSizePx)
            resizeTemplate(tvShipTemplate14, length = 1, cellSizePx)
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

    /**
     * Коллбэк от BattleFieldView.Listener:
     * включаем/выключаем кнопку «В бой!» в зависимости от валидности поля.
     */
    /*override fun onFieldValidityChanged(isValid: Boolean) {
        binding.btnFight.isEnabled = isValid
    }*/

    /**
     * Коллбэк от BattleFieldView.Listener:
     * когда поле просит поворот выбранного корабля.
     */
    /*override fun onRotateRequested(ship: ShipPlacement) {
        // мгновенно поворачиваем текущий выделенный корабль
        binding.bfv.rotateSelected()
    }*/
}