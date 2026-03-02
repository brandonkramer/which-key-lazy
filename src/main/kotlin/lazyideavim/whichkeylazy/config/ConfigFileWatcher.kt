package lazyideavim.whichkeylazy.config

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.vfs.AsyncFileListener
import com.intellij.openapi.vfs.VirtualFileManager

class ConfigFileWatcher {

    fun start() {
        val listener = AsyncFileListener { events ->
            val isRelevant = events.any { event ->
                val path = event.path?.replace('\\', '/') ?: return@any false
                path.endsWith(".whichkey-lazy.json") ||
                    path.endsWith(".ideavimrc") ||
                    path.endsWith("ideavim/ideavimrc")
            }
            if (!isRelevant) return@AsyncFileListener null

            object : AsyncFileListener.ChangeApplier {
                override fun afterVfsChange() {
                    ApplicationManager.getApplication().invokeLater {
                        val service = WhichKeyConfigService.getInstance()
                        service.reload()
                        service.loadError?.let { showConfigError(it) }
                    }
                }
            }
        }
        VirtualFileManager.getInstance().addAsyncFileListener(listener) {}
    }

    private fun showConfigError(error: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("lazyideavim.whichkeylazy")
            .createNotification("Which Key Lazy Config Error", error, NotificationType.WARNING)
            .notify(null)
    }
}
