package lazyideavim.whichkeylazy.config

import lazyideavim.whichkeylazy.ideavim.IdeaVimApiReader
import lazyideavim.whichkeylazy.ideavim.IdeaVimRcParser
import lazyideavim.whichkeylazy.ideavim.IdeaVimTreeBuilder
import lazyideavim.whichkeylazy.model.KeyNode
import lazyideavim.whichkeylazy.model.OverrideEntry
import lazyideavim.whichkeylazy.model.WhichKeySettings
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service

@Service(Service.Level.APP)
class WhichKeyConfigService {

    var settings: WhichKeySettings = WhichKeySettings()
        private set

    var rootBindings: Map<String, KeyNode> = emptyMap()
        private set

    var overrides: Map<String, OverrideEntry> = emptyMap()
        private set

    var loadError: String? = null
        private set

    fun reload() {
        loadConfigOverrides()
        loadBindings()
    }

    private fun loadConfigOverrides() {
        WhichKeyConfig.load().onSuccess { result ->
            overrides = result.overrides
            settings = result.settings
        }
    }

    private fun loadBindings() {
        if (IdeaVimApiReader.isAvailable()) {
            val bindings = IdeaVimApiReader.readMappings()
            if (bindings.isNotEmpty()) {
                rootBindings = bindings
                loadError = null
                return
            }
        }

        // Fallback: parse ~/.ideavimrc file directly
        try {
            val parseResult = IdeaVimRcParser.parse()
            rootBindings = IdeaVimTreeBuilder.buildTree(parseResult)
            loadError = null
        } catch (e: Exception) {
            loadError = e.message ?: "Failed to load IdeaVim mappings"
        }
    }

    companion object {
        fun getInstance(): WhichKeyConfigService {
            return ApplicationManager.getApplication().getService(WhichKeyConfigService::class.java)
        }
    }
}
