package com.example.battleship_game.presentation.profile

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import com.example.battleship_game.common.BaseActivity
import com.example.battleship_game.R
import com.example.battleship_game.databinding.ActivityEditNameBinding
import com.example.battleship_game.dialog.CustomAlertDialog
import com.example.battleship_game.common.UserPreferences.nickname

class EditNameActivity : BaseActivity() {

    private lateinit var binding: ActivityEditNameBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEditNameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // edge-to-edge + immersive
        applyEdgeInsets(binding.main)
        enterImmersiveMode()

        setupListeners()

        // Перехват системной кнопки «Назад»
        onBackPressedDispatcher.addCallback(this) {
            finish()
        }
    }

    private fun setupListeners() {
        binding.apply {
            btnBack.setOnClickListener {
                finish()
            }

            etName.setText(nickname)

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
        val cleaned = raw.trim().replace(Regex("\\s+"), " ")
        val valid = Regex("^[А-Яа-яA-Za-z ]{1,20}$").matches(cleaned) && cleaned.length <= 20

        clearFocusKeyboard()
        if (!valid) {
            binding.tvError.apply {
                text = getString(R.string.hint_name)
                visibility = View.VISIBLE
            }
            showExitConfirmDialog()
        } else {
            // сохраняем и закрываем
            nickname = cleaned
            finish()
        }
    }

    private fun showExitConfirmDialog() {
        CustomAlertDialog(this)
            .setIcon(R.drawable.ic_launcher_foreground)
            .setTitle(R.string.error_name_title)
            .setMessage(R.string.error_name_message)
            .setPositiveButtonText(R.string.action_ok)
            .show()
    }
}