package lazyideavim.whichkeylazy

import lazyideavim.whichkeylazy.config.WhichKeyConfigService
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class WhichKeyStartup : ProjectActivity {

    override suspend fun execute(project: Project) {
        // Pre-load config so it's ready when the VimExtension initializes.
        // The VimExtension (activated via "set which-key") handles the
        // AnActionListener registration and file watcher setup.
        WhichKeyConfigService.getInstance().reload()
    }
}
