package com.example.battleship_game.presentation.placement

import android.os.Bundle
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.example.battleship_game.R
import com.example.battleship_game.common.BaseActivity
import com.example.battleship_game.data.model.ShipPlacement
import com.example.battleship_game.databinding.ActivitySavePlacementBinding
import com.example.battleship_game.dialog.CustomAlertDialog


/**
 * Экран «Сохранить расстановку».
 *
 * Из Intent ожидаем ArrayList<ShipPlacement> по ключу EXTRA_SHIPS.
 * После валидации имени — передаём всё в [SavePlacementViewModel].
 */
class SavePlacementActivity : BaseActivity() {

    companion object {
        const val EXTRA_SHIPS = "EXTRA_SHIPS"
    }

    private lateinit var binding: ActivitySavePlacementBinding
    private val vm: SavePlacementViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySavePlacementBinding.inflate(layoutInflater)
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
            btnBack.setOnClickListener { finish() }

            // Обработчик кнопки «Готово» на клавиатуре:
            etName.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    clearFocusKeyboard()
                    true
                } else {
                    false
                }
            }

            // Физическая клавиша ENTER:
            etName.setOnKeyListener { _, keyCode, keyEvent ->
                if (keyEvent.action == KeyEvent.ACTION_UP
                    && keyCode == KeyEvent.KEYCODE_ENTER
                ) {
                    clearFocusKeyboard()
                    true
                } else {
                    false
                }
            }

            btnSave.setOnClickListener {
                val raw = etName.text.toString()
                // trim + нормализовать пробелы
                val name = raw.trim().replace(Regex("\\s+"), " ")
                // разрешаем буквы, цифры, пробел, длина ≤20
                val valid = Regex("^[\\p{L}\\d ]{1,20}$").matches(name) && name.length <= 20
                if (!valid) {
                    showExitConfirmDialog()
                    return@setOnClickListener
                }
                // достаём список ShipPlacement из Intent
                val ships = intent
                    .getParcelableArrayListExtra<ShipPlacement>(EXTRA_SHIPS)
                    .orEmpty()

                // сохраняем и закрываем
                vm.save(name, ships)
                finish()
            }
        }
    }

    /** Скрывает клавиатуру и снимает фокус с etName */
    private fun clearFocusKeyboard() {
        binding.etName.clearFocus()
        val imm = ContextCompat.getSystemService(
            this, InputMethodManager::class.java
        )
        imm?.hideSoftInputFromWindow(binding.etName.windowToken, 0)
    }

    private fun showExitConfirmDialog() {
        CustomAlertDialog(this)
            .setIcon(R.drawable.ic_launcher_foreground)
            .setTitle(R.string.error_name_title)
            .setMessage(R.string.error_placement_name_message)
            .setPositiveButtonText(R.string.action_ok)
            .show()
    }
}