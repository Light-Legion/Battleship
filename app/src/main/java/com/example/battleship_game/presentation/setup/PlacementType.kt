package com.example.battleship_game.presentation.setup

import android.content.Context
import com.example.battleship_game.R

enum class PlacementType(val displayNameRes: Int) {
    MANUAL(R.string.placement_manual),
    AUTO(R.string.placement_auto),
    LOAD_SAVED(R.string.placement_load_saved);

    companion object {
        fun fromDisplayName(context: Context, name: String): PlacementType? {
            return PlacementType.entries.firstOrNull {
                context.getString(it.displayNameRes) == name
            }
        }

        fun getDisplayNames(context: Context): List<String> {
            return PlacementType.entries.map { context.getString(it.displayNameRes) }
        }
    }
}
