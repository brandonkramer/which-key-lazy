package lazyideavim.whichkeylazy.ui

import lazyideavim.whichkeylazy.config.WhichKeyConfigService
import lazyideavim.whichkeylazy.model.KeyNode
import lazyideavim.whichkeylazy.model.PopupPlacement
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.BorderFactory
import javax.swing.JPanel

/**
 * Purely visual which-key popup. Does not handle key events -- IdeaVim processes
 * all keys, and WhichKeyPopupManager updates this popup externally.
 */
class WhichKeyPopup(
    private val editor: Editor
) {

    private val breadcrumb = BreadcrumbBar()
    private val gridPanel = WhichKeyPanel(WhichKeyConfigService.getInstance().settings)

    private var popup: JBPopup? = null

    val isShowing: Boolean
        get() = popup?.isVisible == true

    fun show(path: List<String>, bindings: Map<String, KeyNode>) {
        val settings = WhichKeyConfigService.getInstance().settings
        val placement = PopupPlacement.fromId(settings.placement)
        val gapX = settings.gapX.coerceAtLeast(0)
        val gapY = settings.gapY.coerceAtLeast(0)

        breadcrumb.updatePath(path)
        gridPanel.setAvailableHeight(availableHeight(gapY))
        gridPanel.updateEntries(bindings)

        val panel = JPanel(BorderLayout()).apply {
            background = WhichKeyColors.PANEL_BG
            border = BorderFactory.createLineBorder(WhichKeyColors.BORDER, 1)
            add(breadcrumb, BorderLayout.NORTH)
            add(gridPanel, BorderLayout.CENTER)
        }

        popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(panel, null)
            .setRequestFocus(false)
            .setCancelKeyEnabled(false)
            .setCancelOnClickOutside(true)
            .setCancelOnOtherWindowOpen(true)
            .setFocusable(false)
            .setMovable(false)
            .setResizable(false)
            .createPopup()

        showAtEditor(panel, placement, gapX, gapY)
    }

    fun close() {
        popup?.cancel()
        popup = null
    }

    private fun availableHeight(gapY: Int): Int {
        val height = editor.contentComponent.visibleRect.height
        return (height - gapY).coerceAtLeast(MIN_HEIGHT.coerceAtMost(height))
    }

    private fun showAtEditor(
        panel: JPanel,
        placement: PopupPlacement,
        gapX: Int,
        gapY: Int
    ) {
        val contentComponent = editor.contentComponent
        val visibleRect = contentComponent.visibleRect

        panel.doLayout()
        val prefSize = panel.preferredSize
        val maxWidth = (visibleRect.width - gapX)
            .coerceAtLeast(MIN_WIDTH.coerceAtMost(visibleRect.width))
        val maxHeight = (visibleRect.height - gapY)
            .coerceAtLeast(MIN_HEIGHT.coerceAtMost(visibleRect.height))
        val popupWidth = prefSize.width.coerceAtLeast(MIN_WIDTH).coerceAtMost(maxWidth)
        val popupHeight = prefSize.height.coerceAtLeast(MIN_HEIGHT).coerceAtMost(maxHeight)
        val popupSize = Dimension(popupWidth, popupHeight)

        popup?.size = popupSize
        popup?.show(RelativePoint(contentComponent, placement.location(visibleRect, popupSize, gapX, gapY)))
    }

    companion object {
        private const val MIN_WIDTH = 300
        private const val MIN_HEIGHT = 80
    }
}
