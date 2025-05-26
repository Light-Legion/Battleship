package com.example.battleship_game.presentation.profile

import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import com.example.battleship_game.common.BaseActivity
import com.example.battleship_game.R
import com.example.battleship_game.databinding.ActivityProfileBinding
import com.example.battleship_game.common.UserPreferences.avatarRes
import com.example.battleship_game.common.UserPreferences.nickname
import com.google.android.material.snackbar.Snackbar

class ProfileActivity : BaseActivity() {

    private lateinit var binding: ActivityProfileBinding

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
                startActivity(Intent(this@ProfileActivity, EditNameActivity::class.java))
                Snackbar.make(main, R.string.edit_name, Snackbar.LENGTH_SHORT).show()
            }

            // открываем экран выбора аватара
            binding.btnChangeAvatar.setOnClickListener {
                startActivity(Intent(this@ProfileActivity, SelectAvatarActivity::class.java))
                Snackbar.make(main, R.string.change_avatar, Snackbar.LENGTH_SHORT).show()
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