package lazyideavim.whichkeylazy.ui

import lazyideavim.whichkeylazy.config.WhichKeyConfigService
import lazyideavim.whichkeylazy.model.KeyNode
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Point
import javax.swing.BorderFactory
import javax.swing.JPanel

/**
 * Purely visual which-key popup. Does not handle key events -- IdeaVim processes
 * all keys, and WhichKeyPopupManager updates this popup externally.
 */
class WhichKeyPopup(
    private val editor: Editor,
    private val dataContext: DataContext
) {

    private val breadcrumb = BreadcrumbBar()
    private val gridPanel = WhichKeyPanel(WhichKeyConfigService.getInstance().settings)

    private var popup: JBPopup? = null
    private var rootPanel: JPanel? = null

    val isShowing: Boolean
        get() = popup?.isVisible == true

    fun show(bindings: Map<String, KeyNode>) {
        gridPanel.setAvailableHeight(availableHeight())

        val panel = JPanel(BorderLayout()).apply {
            background = WhichKeyColors.PANEL_BG
            border = BorderFactory.createLineBorder(WhichKeyColors.BORDER, 1)
            add(breadcrumb, BorderLayout.NORTH)
            add(gridPanel, BorderLayout.CENTER)
        }
        rootPanel = panel

        gridPanel.updateEntries(bindings)

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

        showAtEditorBottom()
    }

    fun updateLevel(path: List<String>, bindings: Map<String, KeyNode>) {
        gridPanel.setAvailableHeight(availableHeight())
        breadcrumb.updatePath(path)
        gridPanel.updateEntries(bindings)

        rootPanel?.let { panel ->
            panel.revalidate()
            panel.repaint()
            val visibleArea = editor.scrollingModel.visibleArea
            val prefSize = panel.preferredSize
            popup?.size = Dimension(
                prefSize.width.coerceAtLeast(300).coerceAtMost(visibleArea.width),
                prefSize.height.coerceAtLeast(80)
            )
        }
    }

    fun close() {
        popup?.cancel()
        popup = null
    }

    private fun availableHeight(): Int {
        return (editor.scrollingModel.visibleArea.height * 0.6).toInt()
    }

    private fun showAtEditorBottom() {
        val panel = rootPanel ?: return
        val contentComponent = editor.contentComponent
        val visibleRect = contentComponent.visibleRect

        panel.doLayout()
        val prefSize = panel.preferredSize
        val popupWidth = prefSize.width.coerceAtLeast(300).coerceAtMost(visibleRect.width)
        val popupHeight = prefSize.height.coerceAtLeast(80)

        val x = visibleRect.x + (visibleRect.width - popupWidth) / 2
        val y = visibleRect.y + visibleRect.height - popupHeight

        popup?.size = Dimension(popupWidth, popupHeight)
        popup?.show(RelativePoint(contentComponent, Point(x, y)))
    }
}
