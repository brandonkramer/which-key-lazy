package lazyideavim.whichkeylazy.model

import java.awt.Dimension
import java.awt.Point
import java.awt.Rectangle

/**
 * Where the popup is anchored inside the visible editor area.
 *
 * The id is the value stored in `~/.whichkey-lazy.json`.
 */
enum class PopupPlacement(val id: String, val displayName: String) {
    BOTTOM_LEFT("bottom-left", "Bottom Left"),
    BOTTOM_CENTER("bottom-center", "Bottom Center"),
    BOTTOM_RIGHT("bottom-right", "Bottom Right"),
    CENTER("center", "Center"),
    TOP_LEFT("top-left", "Top Left"),
    TOP_CENTER("top-center", "Top Center"),
    TOP_RIGHT("top-right", "Top Right");

    /**
     * Top-left corner of a popup of [popupSize] inside [visible].
     *
     * The gap is measured inward from the selected anchor. For center
     * placements it offsets the popup from the center point.
     */
    fun location(visible: Rectangle, popupSize: Dimension, gapX: Int, gapY: Int): Point {
        val width = popupSize.width.coerceIn(0, visible.width)
        val height = popupSize.height.coerceIn(0, visible.height)
        val gx = gapX.coerceAtLeast(0)
        val gy = gapY.coerceAtLeast(0)

        val left = visible.x + gx
        val right = visible.x + visible.width - width - gx
        val top = visible.y + gy
        val bottom = visible.y + visible.height - height - gy
        val centerX = visible.x + (visible.width - width) / 2
        val centerY = visible.y + (visible.height - height) / 2

        val point = when (this) {
            BOTTOM_LEFT -> Point(left, bottom)
            BOTTOM_CENTER -> Point(centerX + gx, bottom)
            BOTTOM_RIGHT -> Point(right, bottom)
            CENTER -> Point(centerX + gx, centerY + gy)
            TOP_LEFT -> Point(left, top)
            TOP_CENTER -> Point(centerX + gx, top)
            TOP_RIGHT -> Point(right, top)
        }

        return Point(
            point.x.coerceIn(visible.x, visible.x + visible.width - width),
            point.y.coerceIn(visible.y, visible.y + visible.height - height)
        )
    }

    override fun toString(): String = displayName

    companion object {
        const val DEFAULT_ID = "bottom-center"
        val DEFAULT = BOTTOM_CENTER

        fun fromId(id: String?): PopupPlacement =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: DEFAULT
    }
}
