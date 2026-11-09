package lazyideavim.whichkeylazy.model

import java.awt.Dimension
import java.awt.Point
import java.awt.Rectangle
import org.junit.Assert.assertEquals
import org.junit.Test

class PopupPlacementTest {

    private val visible = Rectangle(100, 50, 1000, 800)
    private val popup = Dimension(300, 200)

    @Test
    fun `bottom left anchors to bottom left corner`() {
        assertEquals(Point(100, 650), PopupPlacement.BOTTOM_LEFT.location(visible, popup, 0, 0))
    }

    @Test
    fun `bottom center is horizontally centered`() {
        assertEquals(Point(450, 650), PopupPlacement.BOTTOM_CENTER.location(visible, popup, 0, 0))
    }

    @Test
    fun `bottom right anchors to bottom right corner`() {
        assertEquals(Point(800, 650), PopupPlacement.BOTTOM_RIGHT.location(visible, popup, 0, 0))
    }

    @Test
    fun `center is centered in both axes`() {
        assertEquals(Point(450, 350), PopupPlacement.CENTER.location(visible, popup, 0, 0))
    }

    @Test
    fun `top left anchors to top left corner`() {
        assertEquals(Point(100, 50), PopupPlacement.TOP_LEFT.location(visible, popup, 0, 0))
    }

    @Test
    fun `top center is horizontally centered`() {
        assertEquals(Point(450, 50), PopupPlacement.TOP_CENTER.location(visible, popup, 0, 0))
    }

    @Test
    fun `top right anchors to top right corner`() {
        assertEquals(Point(800, 50), PopupPlacement.TOP_RIGHT.location(visible, popup, 0, 0))
    }

    @Test
    fun `gap moves popup inward from the anchor`() {
        assertEquals(Point(110, 630), PopupPlacement.BOTTOM_LEFT.location(visible, popup, 10, 20))
        assertEquals(Point(790, 630), PopupPlacement.BOTTOM_RIGHT.location(visible, popup, 10, 20))
        assertEquals(Point(110, 70), PopupPlacement.TOP_LEFT.location(visible, popup, 10, 20))
        assertEquals(Point(790, 70), PopupPlacement.TOP_RIGHT.location(visible, popup, 10, 20))
    }

    @Test
    fun `gap offsets centered placements`() {
        assertEquals(Point(460, 630), PopupPlacement.BOTTOM_CENTER.location(visible, popup, 10, 20))
        assertEquals(Point(460, 70), PopupPlacement.TOP_CENTER.location(visible, popup, 10, 20))
        assertEquals(Point(460, 370), PopupPlacement.CENTER.location(visible, popup, 10, 20))
    }

    @Test
    fun `negative gap is treated as zero`() {
        assertEquals(Point(100, 650), PopupPlacement.BOTTOM_LEFT.location(visible, popup, -10, -20))
    }

    @Test
    fun `gap is clamped to the visible editor area`() {
        assertEquals(Point(100, 50), PopupPlacement.BOTTOM_RIGHT.location(visible, popup, 2000, 2000))
    }

    @Test
    fun `placement id lookup is case insensitive`() {
        assertEquals(PopupPlacement.TOP_LEFT, PopupPlacement.fromId("TOP-LEFT"))
        assertEquals(PopupPlacement.BOTTOM_RIGHT, PopupPlacement.fromId("bottom-right"))
    }

    @Test
    fun `unknown placement falls back to bottom center`() {
        assertEquals(PopupPlacement.BOTTOM_CENTER, PopupPlacement.fromId("somewhere"))
        assertEquals(PopupPlacement.BOTTOM_CENTER, PopupPlacement.fromId(null))
    }
}
