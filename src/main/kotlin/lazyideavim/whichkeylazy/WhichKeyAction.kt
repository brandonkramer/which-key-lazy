package lazyideavim.whichkeylazy

import lazyideavim.whichkeylazy.config.WhichKeyConfigService
import lazyideavim.whichkeylazy.dispatch.WhichKeyPopupManager
import lazyideavim.whichkeylazy.ideavim.IdeaVimRcParser
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys

/**
 * Manual trigger for the which-key popup (Tools menu).
 * The primary activation method is via "set which-key" in .ideavimrc.
 */
class WhichKeyAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val configService = WhichKeyConfigService.getInstance()

        configService.loadError?.let { error ->
            notify(e, "Config error: $error", NotificationType.WARNING)
            return
        }

        if (configService.rootBindings.isEmpty()) {
            val message = if (!IdeaVimRcParser.IDEAVIMRC_FILE.exists()) {
                "No ~/.ideavimrc found. Create one with leader mappings, e.g.:\n" +
                "let mapleader = \" \"\n" +
                "nmap <leader>ff :action GotoFile<cr>"
            } else {
                "No leader-prefixed action mappings found in .ideavimrc.\n" +
                "Add mappings like: nmap <leader>ff :action GotoFile<cr>"
            }
            notify(e, message, NotificationType.INFORMATION)
            return
        }

        WhichKeyPopupManager.showPopup(editor, emptyList(), configService.rootBindings)
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.getData(CommonDataKeys.EDITOR) != null
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    private fun notify(e: AnActionEvent, message: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("lazyideavim.whichkeylazy")
            .createNotification("Which Key Lazy", message, type)
            .notify(e.project)
    }
}
