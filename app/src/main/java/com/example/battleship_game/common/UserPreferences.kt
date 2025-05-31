package com.example.battleship_game.common

import android.content.Context
import com.example.battleship_game.R
import com.example.battleship_game.data.model.Difficulty
import androidx.core.content.edit

object UserPreferences {

    private const val PREFS_NAME = "user_prefs"
    private const val KEY_NAME = "key_name"
    private const val KEY_AVATAR = "key_avatar"
    private const val KEY_BATTLE_DIFFICULTY = "key_battle_difficulty"

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var Context.nickname: String
        get() = prefs(this).getString(KEY_NAME, getString(R.string.label_player)) ?: getString(R.string.label_player)
        set(v) = prefs(this).edit { putString(KEY_NAME, v) }

    var Context.avatarRes: Int
        get() = prefs(this).getInt(KEY_AVATAR, R.drawable.avatar_male_1)
        set(v) = prefs(this).edit { putInt(KEY_AVATAR, v) }

    var Context.battleDifficulty: Difficulty
        get() {
            val str = prefs(this).getString(KEY_BATTLE_DIFFICULTY, Difficulty.MEDIUM.name)
            return Difficulty.valueOf(str ?: Difficulty.MEDIUM.name)
        }
        set(v) = prefs(this).edit { putString(KEY_BATTLE_DIFFICULTY, v.name) }

}