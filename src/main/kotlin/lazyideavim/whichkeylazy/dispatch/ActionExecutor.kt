package lazyideavim.whichkeylazy.dispatch

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.*
import java.lang.reflect.Method

object ActionExecutor {

    private val performActionMethod: Method? by lazy {
        runCatching {
            Class.forName("com.intellij.openapi.actionSystem.ex.ActionUtil")
                .getMethod(
                    "performActionDumbAwareWithCallbacks",
                    AnAction::class.java,
                    AnActionEvent::class.java
                )
        }.getOrNull()
    }

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
        val method = performActionMethod
        if (method != null) {
            try {
                method.invoke(null, action, event)
            } catch (_: Exception) {
                ActionManager.getInstance().tryToExecute(action, null, null, ActionPlaces.KEYBOARD_SHORTCUT, true)
            }
        } else {
            ActionManager.getInstance().tryToExecute(action, null, null, ActionPlaces.KEYBOARD_SHORTCUT, true)
        }
    }
}
