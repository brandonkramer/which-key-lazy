package lazyideavim.whichkeylazy.config

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class ReloadConfigAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val service = WhichKeyConfigService.getInstance()
        service.reload()

        val (message, type) = if (service.loadError != null) {
            "Config error: ${service.loadError}" to NotificationType.WARNING
        } else {
            "Which Key Lazy config reloaded successfully" to NotificationType.INFORMATION
        }

        NotificationGroupManager.getInstance()
            .getNotificationGroup("lazyideavim.whichkeylazy")
            .createNotification("Which Key Lazy", message, type)
            .notify(e.project)
    }
}
