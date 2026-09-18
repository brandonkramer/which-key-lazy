package lazyideavim.whichkeylazy.dispatch

import lazyideavim.whichkeylazy.config.WhichKeyConfigService
import lazyideavim.whichkeylazy.model.KeyNode
import lazyideavim.whichkeylazy.ui.WhichKeyPopup
import com.intellij.openapi.editor.Editor
import javax.swing.Timer

/**
 * Popup lifecycle manager.
 * Delays the initial popup and updates it in place while navigating groups.
 */
object WhichKeyPopupManager {

    private var popup: WhichKeyPopup? = null
    private var pendingShow: Timer? = null

    val isActive: Boolean get() = popup?.isShowing == true || pendingShow != null

    fun hidePopup() {
        pendingShow?.stop()
        pendingShow = null
        popup?.close()
        popup = null
    }

    fun showPopup(editor: Editor, path: List<String>, entries: Map<String, KeyNode>) {
        if (entries.isEmpty()) {
            hidePopup()
            return
        }

        pendingShow?.stop()
        pendingShow = null

        popup?.let { current ->
            if (current.isShowing && current.isFor(editor)) {
                current.updateLevel(path, entries)
                return
            }
            current.close()
            popup = null
        }

        val delay = WhichKeyConfigService.getInstance().settings.delay.coerceAtLeast(0)
        pendingShow = Timer(delay) {
            pendingShow = null
            if (!editor.isDisposed) {
                val newPopup = WhichKeyPopup(editor)
                popup = newPopup
                newPopup.show(path, entries)
            }
        }.apply {
            isRepeats = false
            start()
        }
    }
}
