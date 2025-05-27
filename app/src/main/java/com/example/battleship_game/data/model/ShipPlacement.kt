package com.example.battleship_game.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Описание расположения одного корабля.
 *
 * @property shipId      Уникальный идентификатор корабля (1–10).
 * @property startRow    Номер строки начала (0–9).
 * @property startCol    Номер столбца начала (0–9).
 * @property length      Количество палуб.
 * @property isVertical  true — вертикально, false — горизонтально.
 */
@Parcelize
data class ShipPlacement(
    val shipId: Int,
    val startRow: Int,
    val startCol: Int,
    val length: Int,
    val isVertical: Boolean
) : Parcelable