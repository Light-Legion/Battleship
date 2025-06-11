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
    private const val KEY_PENDING_GAME_START_TIME = "key_pending_game_start_time"
    private const val KEY_PENDING_GAME_DIFFICULTY = "key_pending_game_difficulty"

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

    var Context.pendingGameStartTime: Long
        get() = prefs(this).getLong(KEY_PENDING_GAME_START_TIME, 0L)
        set(value) = prefs(this).edit { putLong(KEY_PENDING_GAME_START_TIME, value) }

    var Context.pendingGameDifficulty: String?
        get() = prefs(this).getString(KEY_PENDING_GAME_DIFFICULTY, null)
        set(value) {
            if (value != null) {
                prefs(this).edit { putString(KEY_PENDING_GAME_DIFFICULTY, value) }
            } else {
                prefs(this).edit { remove(KEY_PENDING_GAME_DIFFICULTY) }
            }
        }

    fun Context.clearPendingGameFlag() {
        prefs(this).edit {
            remove(KEY_PENDING_GAME_START_TIME)
            remove(KEY_PENDING_GAME_DIFFICULTY)
        }
    }

}