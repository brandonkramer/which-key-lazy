package lazyideavim.whichkeylazy.dispatch

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.actionSystem.Shortcut
import com.intellij.openapi.actionSystem.ex.AnActionListener
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.impl.state.toMappingMode
import com.maddyhome.idea.vim.options.OptionAccessScope
import com.maddyhome.idea.vim.newapi.vim
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

/**
 * Observes keystrokes and manages the which-key popup lifecycle.
 *
 * Reads IdeaVim's KeyHandler state on every keystroke (no self-tracking).
 * Hides popup at start of each handler, then re-shows if nested mappings exist.
 */
class WhichKeyActionListener : AnActionListener {

    override fun beforeEditorTyping(c: Char, dataContext: DataContext) {
        val wasShowing = WhichKeyPopupManager.isShowing
        WhichKeyPopupManager.hidePopup()

        val editor = dataContext.getData(CommonDataKeys.EDITOR) ?: return
        if (!EditorHelper.isFileEditor(editor)) return

        val keyHandlerState = KeyHandler.getInstance().keyHandlerState

        // beforeEditorTyping fires BEFORE IdeaVim processes the key, so append it
        val typedKeySequence = (keyHandlerState.mappingState.keys.toList()
            .ifEmpty { keyHandlerState.commandBuilder.keys.toList() }
                + listOf(KeyStroke.getKeyStroke(c)))
            .dropWhile { it.keyChar.isDigit() }

        if (typedKeySequence.isEmpty()) return

        processKeySequence(editor, typedKeySequence, wasShowingPopup = wasShowing)
    }

    override fun beforeShortcutTriggered(
        shortcut: Shortcut,
        actions: MutableList<AnAction>,
        dataContext: DataContext
    ) {
        val wasShowing = WhichKeyPopupManager.isShowing
        WhichKeyPopupManager.hidePopup()

        if (shortcut !is KeyboardShortcut) return

        val editor = dataContext.getData(CommonDataKeys.EDITOR) ?: return
        if (!EditorHelper.isFileEditor(editor)) return

        val keyHandlerState = KeyHandler.getInstance().keyHandlerState
        val vimCurrentKeySequence = keyHandlerState.mappingState.keys.toList()
            .ifEmpty { keyHandlerState.commandBuilder.keys.toList() }

        val typedKeySequence = vimCurrentKeySequence +
                listOfNotNull(shortcut.firstKeyStroke, shortcut.secondKeyStroke)

        // Space registers as both a shortcut and a typed event -- skip the shortcut variant
        if (typedKeySequence.last() == KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0)) return

        // Shifted printable chars (e.g. "A" = Shift+a) also register as both -- skip shortcut
        val firstStroke = shortcut.firstKeyStroke
        if (firstStroke.keyCode in 32..126
            && (firstStroke.modifiers and KeyEvent.SHIFT_DOWN_MASK) != 0
            && (firstStroke.modifiers and
                    (KeyEvent.CTRL_DOWN_MASK or KeyEvent.ALT_DOWN_MASK or KeyEvent.META_DOWN_MASK) == 0)
        ) return

        processKeySequence(editor, typedKeySequence, wasShowingPopup = wasShowing)
    }

    private fun processKeySequence(
        editor: Editor,
        typedKeySequence: List<KeyStroke>,
        wasShowingPopup: Boolean
    ) {
        val vimEditor = editor.vim
        val mappingMode = vimEditor.mode.toMappingMode()

        val whichKeyOption = injector.optionGroup.getOption("which-key") ?: return
        val optionEnabled = injector.optionGroup.getOptionValue(
            whichKeyOption,
            OptionAccessScope.EFFECTIVE(vimEditor)
        ).asBoolean()
        if (!optionEnabled) return

        // Don't show popup when IdeaVim expects a DIGRAPH argument (after f, t, r, etc.)
        val expectedArgType = KeyHandler.getInstance().keyHandlerState.commandBuilder.expectedArgumentType
        if (expectedArgType == Argument.Type.DIGRAPH) return

        val result = MappingLookup.getNestedEntries(typedKeySequence, mappingMode)

        if (result != null && result.entries.isNotEmpty()) {
            WhichKeyPopupManager.showPopup(editor, result.path, result.entries)
        } else {
            // In INSERT/OP_PENDING modes, unmapped chars should pass through
            if (mappingMode != MappingMode.NORMAL && mappingMode != MappingMode.VISUAL) return

            if (wasShowingPopup && typedKeySequence.size > 1) {
                val lastKey = typedKeySequence.last()
                if (lastKey == KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0)) {
                    KeyHandler.getInstance().reset(vimEditor)
                }
            }
        }
    }
}
