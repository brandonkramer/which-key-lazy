package lazyideavim.whichkeylazy.ui

import lazyideavim.whichkeylazy.config.WhichKeyConfigService
import lazyideavim.whichkeylazy.model.KeyNode
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JPanel
import javax.swing.SwingUtilities

/**
 * Purely visual which-key popup. Does not handle key events -- IdeaVim processes
 * all keys, and WhichKeyPopupManager updates this popup externally.
 */
class WhichKeyPopup(private val editor: Editor) {

    private val breadcrumb = BreadcrumbBar()
    private val gridPanel = WhichKeyPanel(WhichKeyConfigService.getInstance().settings)
    private val panel = JPanel(BorderLayout()).apply {
        background = WhichKeyColors.PANEL_BG
        add(breadcrumb, BorderLayout.NORTH)
        add(gridPanel, BorderLayout.CENTER)
    }

    private var popup: JBPopup? = null

    val isShowing: Boolean get() = popup?.isVisible == true

    fun isFor(candidate: Editor): Boolean = editor === candidate

    fun show(path: List<String>, bindings: Map<String, KeyNode>) {
        updateAvailableSize()
        breadcrumb.updatePath(path)

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
            .setShowBorder(true)
            .setShowShadow(true)
            .createPopup()

        showAtConfiguredPosition()
    }

    fun updateLevel(path: List<String>, bindings: Map<String, KeyNode>) {
        updateAvailableSize()
        breadcrumb.updatePath(path)
        gridPanel.updateEntries(bindings)

        panel.revalidate()
        panel.repaint()
        val popupSize = preferredPopupSize()
        popup?.size = popupSize
        val location = configuredOrigin(popupSize)
        SwingUtilities.convertPointToScreen(location, editor.contentComponent)
        popup?.setLocation(location)
    }

    fun close() {
        popup?.cancel()
        popup = null
    }

    private fun updateAvailableSize() {
        val visibleArea = editor.scrollingModel.visibleArea
        gridPanel.setAvailableSize(visibleArea.width, (visibleArea.height * 0.6).toInt())
    }

    private fun showAtConfiguredPosition() {
        val contentComponent = editor.contentComponent

        panel.doLayout()
        val popupSize = preferredPopupSize()
        val origin = configuredOrigin(popupSize)

        popup?.size = popupSize
        popup?.show(RelativePoint(contentComponent, origin))
    }

    private fun preferredPopupSize(): Dimension {
        val visibleArea = editor.scrollingModel.visibleArea
        return Dimension(
            panel.preferredSize.width.coerceAtLeast(JBUI.scale(300)).coerceAtMost(visibleArea.width),
            panel.preferredSize.height.coerceAtLeast(JBUI.scale(80))
        )
    }

    private fun configuredOrigin(popupSize: Dimension) = PopupGeometry.origin(
        WhichKeyConfigService.getInstance().settings.position,
        editor.contentComponent.visibleRect,
        popupSize,
        editor.visualPositionToXY(editor.caretModel.visualPosition)
    )
}
