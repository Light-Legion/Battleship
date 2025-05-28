package com.example.battleship_game.presentation.placement

import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.example.battleship_game.common.BaseActivity
import com.example.battleship_game.data.model.ShipPlacement
import com.example.battleship_game.databinding.ActivityManualPlacementBinding
import com.example.battleship_game.ui.BattleFieldView

/**
 * Activity для ручной расстановки кораблей.
 *
 * Показывает:
 *  - [BattleFieldView] слева,
 *  - шаблоны кораблей справа (1–2–3–4 в строках),
 *  - кнопки «Назад», «Сохранить», «Очистить», «В бой!».
 */
class ManualPlacementActivity : BaseActivity(),
    BattleFieldView.Listener // имплементируем коллбэки поля
{

    private lateinit var binding: ActivityManualPlacementBinding
    private val vm: ManualPlacementViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityManualPlacementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyEdgeInsets(binding.main)
        enterImmersiveMode()

        setupUI()

        onBackPressedDispatcher.addCallback(this) {
            finish()
        }
    }

    private fun setupUI() {
        binding.apply {
            btnBack.setOnClickListener {
                finish()
            }

            // BattleFieldView: слушаем события
            bfv.listener = this@ManualPlacementActivity

            // Настраиваем RecyclerView шаблонов в GridLayoutManager(4) + SpanSizeLookup
            vm.templates.observe(this@ManualPlacementActivity) { templates ->
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
                rvTemplates.layoutManager = glm
                rvTemplates.adapter = ShipTemplateAdapter(templates) { ship, rawX, rawY ->
                    // стартуем drag из шаблона
                    bfv.externalDragStart(rawX, rawY, ship)
                }
            }

            // Кнопка «Очистить поле»
            btnClear.setOnClickListener {
                bfv.clearAll()
            }

            // В бой!
            btnFight.setOnClickListener {
                val placed = bfv.getPlacedShips()
                // Передаём список в следующую Activity, например, в GameActivity
                /*startActivity(
                    Intent(this@ManualPlacementActivity, GameActivity::class.java)
                        .putParcelableArrayListExtra("EXTRA_SHIPS", ArrayList(placed))
                )*/
            }

            btnSave.setOnClickListener {
                val placed = bfv.getPlacedShips()
                startActivity(
                    Intent(this@ManualPlacementActivity, SavePlacementActivity::class.java)
                        .putParcelableArrayListExtra("EXTRA_SHIPS", ArrayList(placed))
                )
            }
        }
    }

    /**
     * Коллбэк от BattleFieldView.Listener:
     * включаем/выключаем кнопку «В бой!» в зависимости от валидности поля.
     */
    override fun onFieldValidityChanged(isValid: Boolean) {
        binding.btnFight.isEnabled = isValid
    }

    /**
     * Коллбэк от BattleFieldView.Listener:
     * когда поле просит поворот выбранного корабля.
     */
    override fun onRotateRequested(ship: ShipPlacement) {
        // мгновенно поворачиваем текущий выделенный корабль
        binding.bfv.rotateSelected()
    }
}