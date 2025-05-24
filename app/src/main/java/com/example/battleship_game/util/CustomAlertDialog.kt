package com.example.battleship_game.dialog

import android.content.Context
import android.view.LayoutInflater
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import com.example.battleship_game.R
import com.example.battleship_game.databinding.DialogCustomBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class CustomAlertDialog(context: Context) {

    private val binding = DialogCustomBinding.inflate(LayoutInflater.from(context))
    private val dialog = MaterialAlertDialogBuilder(context, R.style.CustomAlertDialog)
        .setView(binding.root)
        .create()

    private var onPositive: (() -> Unit)? = null
    private var onNegative: (() -> Unit)? = null
    private var onDismiss: (() -> Unit)? = null

    init {
        binding.buttonCancel.setOnClickListener {
            onNegative?.invoke()
            dialog.dismiss()
        }
        binding.buttonOk.setOnClickListener {
            onPositive?.invoke()
            dialog.dismiss()
        }
        dialog.setOnDismissListener { onDismiss?.invoke() }
    }

    fun setIcon(@DrawableRes res: Int): CustomAlertDialog {
        binding.dialogIcon.apply {
            setImageResource(res)
            isVisible = true
        }
        return this
    }

    fun setTitle(@StringRes res: Int): CustomAlertDialog {
        binding.dialogTitle.apply {
            setText(res)
            isVisible = true
        }
        return this
    }

    fun setTitle(text: String): CustomAlertDialog {
        binding.dialogTitle.apply {
            this.text = text
            isVisible = true
        }
        return this
    }

    fun setMessage(@StringRes res: Int): CustomAlertDialog {
        binding.dialogMessage.apply {
            setText(res)
            isVisible = true
        }
        return this
    }

    fun setMessage(text: String): CustomAlertDialog {
        binding.dialogMessage.apply {
            this.text = text
            isVisible = true
        }
        return this
    }

    fun setCustomTextItems(items: Array<String>): CustomAlertDialog {
        binding.textGroup.apply {
            removeAllViews()
            items.forEach { item ->
                // создаём TextView программно, но можно вынести в helper
                android.widget.TextView(context).apply {
                    text = item
                    textSize = 16f
                    setTextColor(context.getColor(R.color.black))
                    setPadding(0, 8, 0, 8)
                }.also { addView(it) }
            }
            isVisible = true
        }
        return this
    }

    fun setSingleChoiceItems(items: Array<String>, checked: Int): CustomAlertDialog {
        binding.radioGroupLanguages.apply {
            removeAllViews()
            items.forEachIndexed { idx, str ->
                android.widget.RadioButton(context).apply {
                    text = str
                    isChecked = idx == checked
                }.also { addView(it) }
            }
            isVisible = true
        }
        return this
    }

    fun setPositiveButtonText(@StringRes res: Int): CustomAlertDialog {
        binding.buttonOk.apply {
            setText(res)
            isVisible = true
        }
        return this
    }

    fun setNegativeButtonText(@StringRes res: Int): CustomAlertDialog {
        binding.buttonCancel.apply {
            setText(res)
            isVisible = true
        }
        return this
    }

    fun setOnPositiveClickListener(block: () -> Unit): CustomAlertDialog {
        onPositive = block
        return this
    }
    fun setOnNegativeClickListener(block: () -> Unit): CustomAlertDialog {
        onNegative = block
        return this
    }
    fun setOnDismissListener(block: () -> Unit): CustomAlertDialog {
        onDismiss = block
        return this
    }

    fun show() {
        dialog.show()
    }
}
