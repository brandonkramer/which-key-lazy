package lazyideavim.whichkeylazy.ui

import lazyideavim.whichkeylazy.model.KeyNode
import lazyideavim.whichkeylazy.model.WhichKeySettings
import com.intellij.util.ui.JBUI
import java.awt.*
import javax.swing.JPanel
import kotlin.math.max
import kotlin.math.min

class WhichKeyPanel(
    private val settings: WhichKeySettings
) : JPanel() {

    private var entries: List<KeyNode> = emptyList()
    private var layout = GridLayout(0, 0)
    private var columnWidth = MIN_COL_WIDTH
    private var maxAvailableWidth: Int = Int.MAX_VALUE
    private var maxAvailableHeight: Int = Int.MAX_VALUE

    init {
        isOpaque = false
    }

    fun setAvailableSize(maxWidth: Int, maxHeight: Int) {
        maxAvailableWidth = maxWidth
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
            columnWidth = MIN_COL_WIDTH
            preferredSize = Dimension(0, 0)
            return
        }

        val metrics = getFontMetrics(entryFont())
        columnWidth = computeColumnWidth(metrics)

        // How many rows fit in the available height?
        val maxRows = ((maxAvailableHeight - PADDING * 2) / ROW_HEIGHT).coerceAtLeast(1)

        // Fill top-to-bottom, wrapping at the configured row limit.
        val maxColumns = ((maxAvailableWidth - PADDING * 2) / columnWidth).coerceAtLeast(1)
        layout = PopupGeometry.grid(
            entryCount,
            maxRows,
            maxColumns,
            settings.maxRows,
            settings.maxColumns
        )

        preferredSize = Dimension(
            layout.columns * columnWidth + PADDING * 2,
            layout.rows * ROW_HEIGHT + PADDING * 2
        )
    }

    private fun computeColumnWidth(fm: FontMetrics): Int {
        val showIcons = settings.showIcons
        var maxWidth = MIN_COL_WIDTH
        for (entry in entries) {
            val keyWidth = fm.stringWidth(displayKey(entry.key)) + KEY_BADGE_PADDING * 2
            val descWidth = fm.stringWidth(entry.description)
            val iconWidth = if (showIcons) ICON_SIZE + ICON_RIGHT_MARGIN else 0
            val totalWidth = iconWidth + keyWidth + KEY_DESCRIPTION_GAP + descWidth + ENTRY_PADDING * 2
            maxWidth = max(maxWidth, totalWidth)
        }
        return min(maxWidth, MAX_COL_WIDTH)
    }

    private fun entryFont(): Font {
        return WhichKeyColors.UI_FONT.deriveFont(Font.PLAIN, WhichKeyColors.UI_FONT_SIZE)
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
        val showIcons = settings.showIcons

        val rows = layout.rows

        for ((index, entry) in entries.withIndex()) {
            // Column-major order: fill top-to-bottom, then next column
            val col = index / rows
            val row = index % rows

            val x = PADDING + col * columnWidth
            val y = PADDING + row * ROW_HEIGHT
            val textY = y + (ROW_HEIGHT + fm.ascent - fm.descent) / 2

            var cursorX = x + ENTRY_PADDING

            if (showIcons) {
                val icon = entry.icon
                if (icon != null) {
                    val iconY = y + (ROW_HEIGHT - ICON_SIZE) / 2
                    icon.paintIcon(this, g2, cursorX, iconY)
                }
                // Keep the icon slot reserved so descriptions remain aligned.
                cursorX += ICON_SIZE + ICON_RIGHT_MARGIN
            }

            val keyText = displayKey(entry.key)
            val keyWidth = fm.stringWidth(keyText) + KEY_BADGE_PADDING * 2
            val badgeHeight = ROW_HEIGHT - JBUI.scale(8)
            val badgeY = y + JBUI.scale(4)
            g2.color = WhichKeyColors.KEY_BG
            g2.fillRoundRect(cursorX, badgeY, keyWidth, badgeHeight, JBUI.scale(8), JBUI.scale(8))

            g2.color = WhichKeyColors.KEY_FG
            g2.font = font.deriveFont(Font.BOLD)
            g2.drawString(keyText, cursorX + KEY_BADGE_PADDING, textY)
            g2.font = font

            cursorX += keyWidth + KEY_DESCRIPTION_GAP

            val desc = entry.description
            g2.color = when (entry) {
                is KeyNode.GroupNode -> WhichKeyColors.GROUP_FG
                is KeyNode.ActionNode -> WhichKeyColors.DESC_FG
            }
            val maxDescWidth = columnWidth - (cursorX - x) - ENTRY_PADDING
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
        private val ROW_HEIGHT get() = JBUI.scale(30)
        private val PADDING get() = JBUI.scale(10)
        private val ENTRY_PADDING get() = JBUI.scale(8)
        private val KEY_BADGE_PADDING get() = JBUI.scale(6)
        private val KEY_DESCRIPTION_GAP get() = JBUI.scale(8)
        private val MIN_COL_WIDTH get() = JBUI.scale(152)
        private val MAX_COL_WIDTH get() = JBUI.scale(420)
        private val ICON_SIZE get() = JBUI.scale(16)
        private val ICON_RIGHT_MARGIN get() = JBUI.scale(8)
    }
}
