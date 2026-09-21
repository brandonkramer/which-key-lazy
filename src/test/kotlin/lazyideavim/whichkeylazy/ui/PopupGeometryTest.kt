package lazyideavim.whichkeylazy.ui

import java.awt.Dimension
import java.awt.Point
import java.awt.Rectangle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PopupGeometryTest {
    @Test
    fun `grid wraps at configured row limit`() {
        assertEquals(GridLayout(6, 2), PopupGeometry.grid(12, 20, 5, 7, 5))
    }

    @Test
    fun `grid increases rows when available width limits columns`() {
        assertEquals(GridLayout(17, 3), PopupGeometry.grid(50, 20, 3, 7, 5))
    }

    @Test
    fun `grid handles invalid limits safely`() {
        assertEquals(GridLayout(3, 1), PopupGeometry.grid(3, 0, 0, 0, 0))
    }

    @Test
    fun `grid always has capacity for every entry`() {
        for (entryCount in 1..100) {
            for (availableColumns in 1..6) {
                val layout = PopupGeometry.grid(entryCount, 12, availableColumns, 7, 5)
                assertTrue(layout.rows * layout.columns >= entryCount)
                assertTrue(layout.columns <= availableColumns)
                assertTrue(layout.columns <= 5)
            }
        }
    }

    @Test
    fun `bottom right placement uses an edge margin`() {
        val result = PopupGeometry.origin(
            "bottom-right", Rectangle(100, 50, 1000, 700), Dimension(300, 200), Point()
        )
        assertEquals(Point(788, 546), result)
    }

    @Test
    fun `bottom placement is centered`() {
        val result = PopupGeometry.origin(
            " bottom ", Rectangle(100, 50, 1000, 700), Dimension(300, 200), Point()
        )
        assertEquals(Point(450, 546), result)
    }

    @Test
    fun `cursor placement is clamped to visible editor`() {
        val result = PopupGeometry.origin(
            "cursor", Rectangle(0, 0, 500, 400), Dimension(300, 200), Point(450, 350)
        )
        assertEquals(Point(200, 200), result)
    }
}
