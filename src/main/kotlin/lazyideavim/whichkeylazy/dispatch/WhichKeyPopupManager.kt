package lazyideavim.whichkeylazy.dispatch

import lazyideavim.whichkeylazy.model.KeyNode
import lazyideavim.whichkeylazy.ui.WhichKeyPopup
import com.intellij.openapi.editor.Editor

/**
 * Stateless popup lifecycle manager.
 * Popup is destroyed and recreated on every keystroke.
 */
object WhichKeyPopupManager {

    private var popup: WhichKeyPopup? = null

    val isShowing: Boolean get() = popup != null

    fun hidePopup() {
        popup?.close()
        popup = null
    }

    fun showPopup(editor: Editor, path: List<String>, entries: Map<String, KeyNode>) {
        if (entries.isEmpty()) return

        val newPopup = WhichKeyPopup(editor)
        popup = newPopup
        newPopup.show(path, entries)
    }
}
