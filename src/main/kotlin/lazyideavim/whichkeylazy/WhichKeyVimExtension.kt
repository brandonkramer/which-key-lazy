package lazyideavim.whichkeylazy

import lazyideavim.whichkeylazy.config.ConfigFileWatcher
import lazyideavim.whichkeylazy.config.WhichKeyConfigService
import lazyideavim.whichkeylazy.dispatch.WhichKeyActionListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ex.AnActionListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.maddyhome.idea.vim.extension.VimExtension

/**
 * IdeaVim extension activated via `set which-key` in .ideavimrc.
 * Subscribes an AnActionListener to observe keystrokes and show the popup.
 */
class WhichKeyVimExtension : VimExtension {

    override fun getName() = "which-key"

    override fun init() {
        WhichKeyConfigService.getInstance().reload()
        ConfigFileWatcher().start()

        ApplicationManager.getApplication()
            .messageBus.connect(WhichKeyPluginDisposable.instance)
            .subscribe(AnActionListener.TOPIC, WhichKeyActionListener())
    }
}

@Service(Service.Level.APP)
class WhichKeyPluginDisposable : Disposable {
    companion object {
        val instance: Disposable
            get() = ApplicationManager.getApplication()
                .getService(WhichKeyPluginDisposable::class.java)
    }

    override fun dispose() {}
}
