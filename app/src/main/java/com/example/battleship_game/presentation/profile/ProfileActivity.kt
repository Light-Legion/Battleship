package com.example.battleship_game.presentation.profile

import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import com.example.battleship_game.common.BaseActivity
import com.example.battleship_game.R
import com.example.battleship_game.databinding.ActivityProfileBinding
import com.example.battleship_game.common.UserPreferences.avatarRes
import com.example.battleship_game.common.UserPreferences.nickname
import com.google.android.material.snackbar.Snackbar

class ProfileActivity : BaseActivity() {

    private lateinit var binding: ActivityProfileBinding

    private val editNameLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // Показываем сообщение только если имя было изменено
            Snackbar.make(binding.main, R.string.hint_changed_name, Snackbar.LENGTH_SHORT).show()
        }
    }

    private val changeAvatarLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // Показываем сообщение только если аватар был изменен
            Snackbar.make(binding.main, R.string.hint_changed_avatar, Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityProfileBinding.inflate(layoutInflater)
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

            // открываем экран редактирования имени
            binding.btnEditName.setOnClickListener {
                editNameLauncher.launch(Intent(this@ProfileActivity, EditNameActivity::class.java))
            }

            // открываем экран выбора аватара
            binding.btnChangeAvatar.setOnClickListener {
                changeAvatarLauncher.launch(Intent(this@ProfileActivity, SelectAvatarActivity::class.java))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // при возврате обновляем UI
        binding.ivAvatar.setImageResource(this.avatarRes)
        binding.tvName.text = this.nickname
    }
}