package lazyideavim.whichkeylazy.ui

import lazyideavim.whichkeylazy.model.KeyNode
import lazyideavim.whichkeylazy.model.WhichKeySettings
import java.awt.*
import javax.swing.JPanel
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

class WhichKeyPanel(
    private val settings: WhichKeySettings
) : JPanel() {

    private var entries: List<KeyNode> = emptyList()
    private var maxAvailableHeight: Int = Int.MAX_VALUE

    init {
        isOpaque = true
        background = WhichKeyColors.PANEL_BG
    }

    fun setAvailableHeight(maxHeight: Int) {
        maxAvailableHeight = maxHeight
    }

    fun updateEntries(bindings: Map<String, KeyNode>) {
        entries = sortEntries(bindings.values.toList())
        recomputeSize()
        repaint()
    }

    private fun sortEntries(nodes: List<KeyNode>): List<KeyNode> {
        return if (settings.sortGroupsFirst) {
            val groups = nodes.filterIsInstance<KeyNode.GroupNode>().sortedBy { it.key }
            val actions = nodes.filterIsInstance<KeyNode.ActionNode>().sortedBy { it.key }
            groups + actions
        } else {
            nodes.sortedBy { it.key }
        }
    }

    private fun recomputeSize() {
        val entryCount = entries.size
        if (entryCount == 0) {
            preferredSize = Dimension(0, 0)
            return
        }

        val metrics = getFontMetrics(entryFont())
        val colWidth = computeColumnWidth(metrics)

        // How many rows fit in the available height?
        val maxRows = ((maxAvailableHeight - PADDING * 2) / ROW_HEIGHT).coerceAtLeast(1)

        // Vertical-first: use single column unless entries overflow
        val rows = min(entryCount, maxRows)
        val cols = ceil(entryCount.toDouble() / rows).toInt()

        preferredSize = Dimension(
            cols * colWidth + PADDING * 2,
            rows * ROW_HEIGHT + PADDING * 2
        )
    }

    private fun computeColumnWidth(fm: FontMetrics): Int {
        val showIcons = settings.showIcons
        var maxWidth = MIN_COL_WIDTH
        for (entry in entries) {
            val keyWidth = fm.stringWidth(displayKey(entry.key)) + KEY_BADGE_PADDING * 2
            val descWidth = fm.stringWidth(entry.description)
            val separatorWidth = fm.stringWidth(" \u279C ")
            val iconWidth = if (showIcons) ICON_SIZE + ICON_RIGHT_MARGIN else 0
            val totalWidth = iconWidth + keyWidth + separatorWidth + descWidth + ENTRY_PADDING * 2
            maxWidth = max(maxWidth, totalWidth)
        }
        return min(maxWidth, MAX_COL_WIDTH)
    }

    private fun entryFont(): Font {
        return WhichKeyColors.EDITOR_FONT.deriveFont(Font.PLAIN, WhichKeyColors.EDITOR_FONT_SIZE.toFloat())
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        if (entries.isEmpty()) return

        val g2 = g.create() as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        val font = entryFont()
        g2.font = font
        val fm = g2.fontMetrics
        val colWidth = computeColumnWidth(fm)
        val showIcons = settings.showIcons

        // Compute layout: how many rows per column
        val maxRows = ((maxAvailableHeight - PADDING * 2) / ROW_HEIGHT).coerceAtLeast(1)
        val rows = min(entries.size, maxRows)

        for ((index, entry) in entries.withIndex()) {
            // Column-major order: fill top-to-bottom, then next column
            val col = index / rows
            val row = index % rows

            val x = PADDING + col * colWidth
            val y = PADDING + row * ROW_HEIGHT
            val textY = y + (ROW_HEIGHT + fm.ascent - fm.descent) / 2

            var cursorX = x + ENTRY_PADDING

            if (showIcons) {
                val icon = entry.icon
                if (icon != null) {
                    val iconY = y + (ROW_HEIGHT - ICON_SIZE) / 2
                    icon.paintIcon(this, g2, cursorX, iconY)
                } else {
                    // Draw a small white dot as placeholder for alignment
                    val dotSize = 6
                    val dotX = cursorX + (ICON_SIZE - dotSize) / 2
                    val dotY = y + (ROW_HEIGHT - dotSize) / 2
                    g2.color = WhichKeyColors.SEPARATOR
                    g2.fillOval(dotX, dotY, dotSize, dotSize)
                }
                cursorX += ICON_SIZE + ICON_RIGHT_MARGIN
            }

            val keyText = displayKey(entry.key)
            val keyWidth = fm.stringWidth(keyText) + KEY_BADGE_PADDING * 2
            val badgeHeight = ROW_HEIGHT - 6
            val badgeY = y + 3
            g2.color = WhichKeyColors.KEY_BG
            g2.fillRoundRect(cursorX, badgeY, keyWidth, badgeHeight, 6, 6)

            g2.color = WhichKeyColors.KEY_FG
            g2.font = font.deriveFont(Font.BOLD)
            g2.drawString(keyText, cursorX + KEY_BADGE_PADDING, textY)
            g2.font = font

            cursorX += keyWidth + 4
            g2.color = WhichKeyColors.SEPARATOR
            g2.drawString("\u279C", cursorX, textY)
            cursorX += fm.stringWidth("\u279C") + 4

            val desc = entry.description
            g2.color = when (entry) {
                is KeyNode.GroupNode -> WhichKeyColors.GROUP_FG
                is KeyNode.ActionNode -> WhichKeyColors.DESC_FG
            }
            val maxDescWidth = colWidth - (cursorX - x) - ENTRY_PADDING
            val clippedDesc = clipText(desc, fm, maxDescWidth)
            g2.drawString(clippedDesc, cursorX, textY)
        }

        g2.dispose()
    }

    private fun displayKey(notation: String): String {
        // Single printable character (most common case)
        if (notation.length == 1) {
            return when (notation[0]) {
                ' ' -> "␣"
                else -> notation
            }
        }

        // Special key notations like <Tab>, <C-n>, <BS>, etc.
        val lower = notation.lowercase().removeSurrounding("<", ">")
        return when (lower) {
            "tab" -> "⇥"
            "space" -> "␣"
            "bs", "backspace" -> "⌫"
            "del", "delete" -> "Del"
            "cr", "enter", "return" -> "⏎"
            "esc", "escape" -> "Esc"
            "left" -> "←"
            "right" -> "→"
            "up" -> "↑"
            "down" -> "↓"
            "home" -> "Home"
            "end" -> "End"
            "pageup" -> "PgUp"
            "pagedown" -> "PgDn"
            else -> {
                // F-keys: "f1" → "F1", "f12" → "F12"
                if (lower.startsWith("f") && lower.removePrefix("f").toIntOrNull() != null) {
                    return lower.uppercase()
                }
                // Modified keys like <C-n>, <D-\>, <S-Tab> — show without angle brackets
                notation.removeSurrounding("<", ">")
            }
        }
    }

    private fun clipText(text: String, fm: FontMetrics, maxWidth: Int): String {
        if (maxWidth <= 0) return ""
        if (fm.stringWidth(text) <= maxWidth) return text
        val ellipsis = "\u2026"
        val ellipsisWidth = fm.stringWidth(ellipsis)
        for (i in text.length - 1 downTo 0) {
            if (fm.stringWidth(text.substring(0, i)) + ellipsisWidth <= maxWidth) {
                return text.substring(0, i) + ellipsis
            }
        }
        return ellipsis
    }

    companion object {
        private const val ROW_HEIGHT = 28
        private const val PADDING = 8
        private const val ENTRY_PADDING = 8
        private const val KEY_BADGE_PADDING = 6
        private const val MIN_COL_WIDTH = 140
        private const val MAX_COL_WIDTH = 420
        private const val ICON_SIZE = 16
        private const val ICON_RIGHT_MARGIN = 6
    }
}
