package com.example.battleship_game.profile

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.addCallback
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.battleship_game.BaseActivity
import com.example.battleship_game.R
import com.example.battleship_game.databinding.ActivitySelectAvatarBinding
import com.example.battleship_game.util.UserPreferences.avatarRes

class SelectAvatarActivity : BaseActivity() {

    private lateinit var binding: ActivitySelectAvatarBinding
    private lateinit var adapter: AvatarAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySelectAvatarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyEdgeInsets(binding.main)
        enterImmersiveMode()

        setupListeners()

        onBackPressedDispatcher.addCallback(this) {
            finish()
        }
    }

    @SuppressLint("Recycle")
    private fun setupListeners() {
        // 1) Вытаскиваем список из res/values/avatars.xml
        val avatars = resources.obtainTypedArray(R.array.avatar_drawables).let {
            List(it.length()) { idx -> it.getResourceId(idx, R.drawable.ic_launcher_foreground) }
        }

        // 2) Определяем текущий выбранный индекс
        var selectedIdx = avatars.indexOf(avatarRes).coerceAtLeast(0)

        binding.apply {
            // кнопка "Назад"
            btnBack.setOnClickListener {
                finish()
            }

            // RecyclerView
            rvAvatars.layoutManager = LinearLayoutManager(
                this@SelectAvatarActivity,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            adapter = AvatarAdapter(avatars, selectedIdx) { pos -> selectedIdx = pos }
            rvAvatars.adapter = adapter
            rvAvatars.scrollToPosition(selectedIdx)

            // Кнопка "Сохранить"
            btnSave.setOnClickListener {
                avatarRes = avatars.getOrNull(selectedIdx) ?: R.drawable.ic_launcher_foreground
                finish()
            }
        }
    }
}
