package lazyideavim.whichkeylazy.dispatch

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.ActionUtil

object ActionExecutor {

    fun execute(actionId: String, dataContext: DataContext) {
        val action = ActionManager.getInstance().getAction(actionId)
        if (action == null) {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("lazyideavim.whichkeylazy")
                .createNotification(
                    "Which Key Lazy",
                    "Unknown action: $actionId",
                    NotificationType.WARNING
                )
                .notify(null)
            return
        }

        val event = AnActionEvent.createEvent(
            action,
            dataContext,
            null,
            ActionPlaces.KEYBOARD_SHORTCUT,
            ActionUiKind.NONE,
            null
        )
        ActionUtil.performActionDumbAwareWithCallbacks(action, event)
    }
}
