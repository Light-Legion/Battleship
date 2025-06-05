package com.example.battleship_game.data.model

import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

/**
 * UI-модель для ручного перетаскивания корабля.
 *
 * Содержит «чистую» модель [ShipPlacement] и дополнительные поля,
 * актуальные только во время drag&drop и визуального рендеринга.
 */
@Parcelize
data class ShipPlacementUi(
    /** Базовая «чистая» модель, используемая в логике авто-расстановки и DAO */
    val base: ShipPlacement,

    /** Локальные координаты «под пальцем» (в пикселях относительно ManualPlacementFieldView) */
    @IgnoredOnParcel var tempX: Float = 0f,

    @IgnoredOnParcel var tempY: Float = 0f,

    /** true, если этот кораблик только что «взяли» из шаблона и ещё не зафиксировали */
    @IgnoredOnParcel var fromTemplate: Boolean = true,

    /** true, если текущее положение вызывает коллизию или вылезает за границы */
    @IgnoredOnParcel var isInvalid: Boolean = false
) : Parcelable {
    // В качестве удобства можно «пробросить» свойства из base, например:
    val shipId: Int get() = base.shipId
    val length: Int get() = base.length

    var startRow: Int
        get() = base.startRow
        set(value) { base.startRow = value }

    var startCol: Int
        get() = base.startCol
        set(value) { base.startCol = value }

    var isVertical: Boolean
        get() = base.isVertical
        set(value) { base.isVertical = value }

    companion object {
        /**
         * Упрощённое создание экземпляра из параметров.
         */
        fun from(
            shipId: Int,
            length: Int,
            startRow: Int = 0,
            startCol: Int = 0,
            isVertical: Boolean = false,
            fromTemplate: Boolean = true
        ): ShipPlacementUi {
            val base = ShipPlacement(shipId, length, startRow, startCol, isVertical)
            return ShipPlacementUi(base = base, fromTemplate = fromTemplate)
        }
    }
}

