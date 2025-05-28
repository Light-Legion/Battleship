package com.example.battleship_game.data.model

import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

/**
 * Описание расположения одного корабля на поле.
 *
 * @property shipId      Уникальный идентификатор корабля (1–10).
 * @property length      Количество палуб.
 *
 * Поля ниже служат для динамики в `BattleFieldView`:
 * @property startRow    Текущая строка верхнего/левого конца корабля (0–9).
 * @property startCol    Текущий столбец верхнего/левого конца корабля (0–9).
 * @property isVertical  Ориентация: true — вертикально, false — горизонтально.
 * @property tempX       Смещение по X во время drag-and-drop.
 * @property tempY       Смещение по Y во время drag-and-drop.
 * @property hasConflict Пометка о конфликте (пересечении с другим кораблём).
 */
@Parcelize
data class ShipPlacement(
    val shipId: Int,
    val length: Int,
    var startRow: Int = 0,
    var startCol: Int = 0,
    var isVertical: Boolean = false,

    // эти поля не сохраняются в Parcelable через @Transient
    @IgnoredOnParcel var tempX: Float = 0f,
    @IgnoredOnParcel var tempY: Float = 0f,
    @IgnoredOnParcel var hasConflict: Boolean = false
) : Parcelable