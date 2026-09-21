package lazyideavim.whichkeylazy.ui

import java.awt.Dimension
import java.awt.Point
import java.awt.Rectangle
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

internal data class GridLayout(val rows: Int, val columns: Int)

internal object PopupGeometry {
    private const val HORIZONTAL_MARGIN = 12
    private const val BOTTOM_MARGIN = 4

    fun grid(
        entryCount: Int,
        availableRows: Int,
        availableColumns: Int,
        maxRows: Int,
        maxColumns: Int
    ): GridLayout {
        if (entryCount <= 0) return GridLayout(0, 0)

        val rowLimit = min(availableRows.coerceAtLeast(1), maxRows.coerceAtLeast(1))
        val columnLimit = min(availableColumns.coerceAtLeast(1), maxColumns.coerceAtLeast(1))
        val columns = ceil(entryCount.toDouble() / rowLimit).toInt().coerceAtMost(columnLimit)
        val rows = ceil(entryCount.toDouble() / columns).toInt()
        return GridLayout(rows, columns)
    }

    fun origin(
        position: String,
        visibleArea: Rectangle,
        popupSize: Dimension,
        cursor: Point
    ): Point {
        val left = visibleArea.x + HORIZONTAL_MARGIN
        val right = visibleArea.x + visibleArea.width - popupSize.width - HORIZONTAL_MARGIN
        val bottom = visibleArea.y + visibleArea.height - popupSize.height - BOTTOM_MARGIN
        val centered = visibleArea.x + (visibleArea.width - popupSize.width) / 2

        val raw = when (position.trim().lowercase(Locale.ROOT)) {
            "bottom-left" -> Point(left, bottom)
            "bottom", "bottom-center" -> Point(centered, bottom)
            "cursor" -> Point(cursor.x + HORIZONTAL_MARGIN, cursor.y + HORIZONTAL_MARGIN)
            else -> Point(right, bottom)
        }

        return Point(
            raw.x.coerceIn(visibleArea.x, visibleArea.x + max(0, visibleArea.width - popupSize.width)),
            raw.y.coerceIn(visibleArea.y, visibleArea.y + max(0, visibleArea.height - popupSize.height))
        )
    }
}
