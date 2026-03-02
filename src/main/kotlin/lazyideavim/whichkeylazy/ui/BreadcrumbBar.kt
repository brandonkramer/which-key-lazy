package lazyideavim.whichkeylazy.ui

import java.awt.*
import java.awt.geom.Path2D
import java.awt.geom.RoundRectangle2D
import javax.swing.JPanel
import javax.swing.Timer

class BreadcrumbBar : JPanel() {

    private var segments: List<String> = listOf("Which Key")
    private var cursorVisible = true
    private val blinkTimer = Timer(750) {
        cursorVisible = !cursorVisible
        repaint()
    }

    init {
        isOpaque = true
        background = WhichKeyColors.PANEL_BG
        preferredSize = Dimension(0, 40)
        blinkTimer.start()
    }

    fun updatePath(path: List<String>) {
        segments = listOf("Which Key") + path
        repaint()
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g.create() as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        val font = WhichKeyColors.EDITOR_FONT.deriveFont(Font.BOLD, WhichKeyColors.EDITOR_FONT_SIZE.toFloat())
        g2.font = font
        val fm = g2.fontMetrics
        var x = 13
        val y = (height + fm.ascent - fm.descent) / 2

        val iconSize = 24
        val iconY = (height - iconSize) / 2
        paintLogo(g2, x, iconY, iconSize)
        x += iconSize + 6

        for ((i, segment) in segments.withIndex()) {
            if (i > 0) {
                g2.color = WhichKeyColors.SEPARATOR
                g2.drawString(" \u00BB ", x, y)
                x += fm.stringWidth(" \u00BB ")
            }
            g2.color = WhichKeyColors.BREADCRUMB
            g2.drawString(segment, x, y)
            x += fm.stringWidth(segment)
        }

        // Bottom border line
        g2.color = WhichKeyColors.BORDER
        g2.fillRect(0, height - 1, width, 1)

        g2.dispose()
    }

    private fun paintLogo(g2: Graphics2D, x: Int, y: Int, size: Int) {
        val s = size / 16.0

        // Key cap background
        g2.color = WhichKeyColors.PANEL_BG
        g2.fill(RoundRectangle2D.Double(x + s, y + s, 14 * s, 14 * s, 6 * s, 6 * s))

        // Key cap border
        g2.color = WhichKeyColors.BREADCRUMB
        g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f)
        g2.stroke = BasicStroke((0.5 * s).toFloat())
        g2.draw(RoundRectangle2D.Double(x + s, y + s, 14 * s, 14 * s, 6 * s, 6 * s))
        g2.composite = AlphaComposite.SrcOver

        // Chevron
        g2.color = WhichKeyColors.BREADCRUMB
        g2.stroke = BasicStroke((1.5 * s).toFloat(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        val chevron = Path2D.Double().apply {
            moveTo(x + 4.5 * s, y + 5 * s)
            lineTo(x + 7.5 * s, y + 8 * s)
            lineTo(x + 4.5 * s, y + 11 * s)
        }
        g2.draw(chevron)

        // Blinking cursor
        if (cursorVisible) {
            g2.color = WhichKeyColors.KEY_FG
            g2.fillRect(
                (x + 9 * s).toInt(),
                (y + 10 * s).toInt(),
                (3 * s).toInt(),
                (1.5 * s).toInt()
            )
        }
    }

    override fun removeNotify() {
        super.removeNotify()
        blinkTimer.stop()
    }
}
