package lazyideavim.whichkeylazy.dispatch

import lazyideavim.whichkeylazy.model.KeyNode
import lazyideavim.whichkeylazy.ui.WhichKeyPopup
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
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

        val dataContext = SimpleDataContext.builder()
            .add(CommonDataKeys.EDITOR, editor)
            .add(CommonDataKeys.PROJECT, editor.project)
            .build()

        val newPopup = WhichKeyPopup(editor, dataContext)
        popup = newPopup
        newPopup.show(entries)
        if (path.isNotEmpty()) {
            newPopup.updateLevel(path, entries)
        }
    }
}
