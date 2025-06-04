package com.example.battleship_game.common

import android.content.Context
import androidx.core.content.edit
import com.example.battleship_game.R

object UserPreferences {

    private const val PREFS_NAME = "user_prefs"
    private const val KEY_NAME = "key_name"
    private const val KEY_AVATAR = "key_avatar"
    private const val KEY_MUSIC_ENABLED = "key_music_enabled"
    private const val KEY_LAST_TRACK_INDEX = "key_last_track_index"

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var Context.nickname: String
        get() = prefs(this).getString(KEY_NAME, getString(R.string.label_player)) ?: getString(R.string.label_player)
        set(v) = prefs(this).edit { putString(KEY_NAME, v) }

    var Context.avatarRes: Int
        get() = prefs(this).getInt(KEY_AVATAR, R.drawable.avatar_male_1)
        set(v) = prefs(this).edit { putInt(KEY_AVATAR, v) }

    var Context.isMusicEnabled: Boolean
        get() = prefs(this).getBoolean(KEY_MUSIC_ENABLED, false)
        set(value) = prefs(this).edit { putBoolean(KEY_MUSIC_ENABLED, value) }

    var Context.lastTrackIndex: Int
        get() = prefs(this).getInt(KEY_LAST_TRACK_INDEX, 0)
        set(idx) = prefs(this).edit { putInt(KEY_LAST_TRACK_INDEX, idx) }

}