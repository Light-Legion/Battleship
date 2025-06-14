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
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    private fun setupListeners() {
        binding.apply {
            btnBack.setOnClickListener {
                setResult(RESULT_CANCELED)
                finish()
            }

            etName.hint = nickname

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
        val name = raw.trim().replace(Regex("\\s+"), " ")
        val valid = Regex("^[А-Яа-яA-Za-z ]{1,20}$").matches(name)

        clearFocusKeyboard()
        if (!valid) {
            binding.tvError.apply {
                text = getString(R.string.hint_username)
                visibility = View.VISIBLE
            }
            showExitConfirmDialog()
            return
        }

        // сохраняем и закрываем
        nickname = name

        setResult(RESULT_OK)
        finish()
    }

    private fun showExitConfirmDialog() {
        CustomAlertDialog(this)
            .setIcon(R.drawable.ic_dialog_warning)
            .setTitle(R.string.error_name_title)
            .setMessage(R.string.error_username_message)
            .setPositiveButtonText(R.string.action_ok)
            .show()
    }
}