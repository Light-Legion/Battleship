package com.example.battleship_game.common

import android.content.Context
import com.example.battleship_game.R

object UserPreferences {

    private const val PREFS_NAME = "user_prefs"
    private const val KEY_NAME = "key_name"
    private const val KEY_AVATAR = "key_avatar"
    private const val KEY_BATTLE_DIFFICULTY = "key_battle_difficulty"

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var Context.nickname: String
        get() = prefs(this).getString(KEY_NAME, getString(R.string.player)) ?: getString(R.string.player)
        set(v) = prefs(this).edit().putString(KEY_NAME, v).apply()

    var Context.avatarRes: Int
        get() = prefs(this).getInt(KEY_AVATAR, R.drawable.avatar_male_1)
        set(v) = prefs(this).edit().putInt(KEY_AVATAR, v).apply()

    var Context.battleDifficulty: String
        get() = prefs(this).getString(KEY_BATTLE_DIFFICULTY, getString(R.string.level_medium))
            ?: getString(R.string.level_medium)
        set(v) = prefs(this).edit().putString(KEY_BATTLE_DIFFICULTY, v).apply()

}