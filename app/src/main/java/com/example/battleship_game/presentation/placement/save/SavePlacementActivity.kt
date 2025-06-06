package com.example.battleship_game.presentation.placement.save

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
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
    private val viewModel: SavePlacementViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySavePlacementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyEdgeInsets(binding.main)
        enterImmersiveMode()

        setupUI()

        onBackPressedDispatcher.addCallback(this) {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    private fun setupUI() {
        binding.apply {
            btnBack.setOnClickListener {
                setResult(RESULT_CANCELED)
                finish()
            }

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
                validateName()
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

    private fun validateName() {
        val raw = binding.etName.text.toString()
        // trim + нормализовать пробелы
        val name = raw.trim().replace(Regex("\\s+"), " ")
        // разрешаем буквы, цифры, пробел, длина ≤20
        val valid = Regex("^[\\p{L}\\d ]{1,20}$").matches(name)
        if (!valid) {
            binding.tvError.apply {
                text = getString(R.string.hint_error_placement)
                visibility = View.VISIBLE
            }
            showExitConfirmDialog()
            return
        }
        // достаём список ShipPlacement из Intent
        val ships = intent
            .getParcelableArrayListExtra<ShipPlacement>(EXTRA_SHIPS)
            .orEmpty()

        // сохраняем и закрываем
        viewModel.save(name, ships)

        setResult(RESULT_OK)
        finish()
    }

    private fun showExitConfirmDialog() {
        CustomAlertDialog(this)
            .setIcon(R.drawable.ic_dialog_warning)
            .setTitle(R.string.error_name_title)
            .setMessage(R.string.error_placement_name_message)
            .setPositiveButtonText(R.string.action_ok)
            .show()
    }
}