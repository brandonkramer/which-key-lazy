package lazyideavim.whichkeylazy.config

import lazyideavim.whichkeylazy.model.*
import lazyideavim.whichkeylazy.ui.IconResolver
import kotlinx.serialization.json.Json
import java.io.File

object WhichKeyConfig {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    val CONFIG_FILE: File
        get() = File(System.getProperty("user.home"), ".whichkey-lazy.json")

    data class LoadResult(
        val settings: WhichKeySettings,
        val bindings: Map<String, KeyNode>,
        val overrides: Map<String, OverrideEntry>
    )

    fun load(): Result<LoadResult> {
        return try {
            val text = if (CONFIG_FILE.exists()) {
                CONFIG_FILE.readText()
            } else {
                // No user config — use bundled defaults
                DefaultConfig.JSON_TEMPLATE
            }
            val root = json.decodeFromString<WhichKeyRoot>(text)
            val bindings = convertBindings(root.bindings)
            Result.success(LoadResult(root.settings, bindings, root.overrides))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun convertBindings(entries: Map<String, BindingEntry>): Map<String, KeyNode> {
        return entries.mapNotNull { (keyStr, entry) ->
            if (keyStr.isEmpty()) return@mapNotNull null
            val node = convertEntry(keyStr, entry) ?: return@mapNotNull null
            keyStr to node
        }.toMap()
    }

    private fun convertEntry(key: String, entry: BindingEntry): KeyNode? {
        val children = entry.children
        val actionId = entry.actionId

        if (!children.isNullOrEmpty() && actionId.isNullOrBlank()) {
            return KeyNode.GroupNode(
                key = key,
                description = entry.description,
                icon = IconResolver.resolveGroupIcon(entry.description, entry.icon),
                children = convertBindings(children)
            )
        }

        if (!actionId.isNullOrBlank() && children.isNullOrEmpty()) {
            return KeyNode.ActionNode(
                key = key,
                description = entry.description,
                icon = IconResolver.resolveActionIcon(actionId),
                actionId = actionId
            )
        }

        return null
    }
}
