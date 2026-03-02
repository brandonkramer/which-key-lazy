package lazyideavim.whichkeylazy.dispatch

import lazyideavim.whichkeylazy.ActionDescriptions
import lazyideavim.whichkeylazy.config.WhichKeyConfigService
import lazyideavim.whichkeylazy.model.KeyNode
import lazyideavim.whichkeylazy.model.OverrideEntry
import lazyideavim.whichkeylazy.ui.IconResolver
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.DuplicableOperatorAction
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.key.MappingInfo
import com.maddyhome.idea.vim.key.ToActionMappingInfo
import com.maddyhome.idea.vim.key.ToKeysMappingInfo
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

/**
 * Queries IdeaVim for nested entries at any key sequence prefix.
 */
object MappingLookup {

    data class LookupResult(
        val path: List<String>,
        val entries: Map<String, KeyNode>
    )

    private val VIM_ACTIONS: Map<MappingMode, Map<List<KeyStroke>, String>> by lazy {
        val result = mutableMapOf<MappingMode, MutableMap<List<KeyStroke>, String>>()
        try {
            for (mode in enumValues<MappingMode>()) {
                val modeMap = result.getOrPut(mode) { mutableMapOf() }
                injector.keyGroup.getBuiltinCommandsTrie(mode).getEntries()
                    .forEach { entry ->
                        if (entry.data == null) return@forEach

                        var current = entry
                        val keyStrokes = mutableListOf(current.key)
                        while (current.parent != null && current.parent!!.parent != null) {
                            current = current.parent!!
                            keyStrokes.add(0, current.key)
                        }

                        if (keyStrokes.size > 1) {
                            modeMap[keyStrokes.toList()] = entry.data!!.actionId
                        }
                    }
            }
        } catch (_: Exception) {}
        result
    }

    fun getNestedEntries(
        typedKeySequence: List<KeyStroke>,
        mappingMode: MappingMode = MappingMode.NORMAL
    ): LookupResult? {
        if (typedKeySequence.isEmpty()) return null

        val leaderResult = if (mappingMode == MappingMode.NORMAL || mappingMode == MappingMode.VISUAL) {
            tryLeaderLookup(typedKeySequence)
        } else {
            null
        }

        val mappingsResult = queryAllMappings(typedKeySequence, mappingMode)

        val result = if (leaderResult != null && mappingsResult != null) {
            val merged = mappingsResult.entries.mapValues { (key, runtimeNode) ->
                leaderResult.entries[key] ?: runtimeNode
            }
            LookupResult(leaderResult.path, merged)
        } else {
            leaderResult ?: mappingsResult
        } ?: return null

        val postLeaderPrefix = computePostLeaderPrefix(typedKeySequence)
        val overridden = applyOverrides(result.entries, postLeaderPrefix)

        return LookupResult(result.path, overridden)
    }

    private fun computePostLeaderPrefix(typedKeySequence: List<KeyStroke>): String? {
        val postLeaderKeys = findPostLeaderKeys(typedKeySequence) ?: return null
        return postLeaderKeys.mapNotNull { keyToString(it) }.joinToString("")
    }

    private fun findPostLeaderKeys(typedKeySequence: List<KeyStroke>): List<KeyStroke>? {
        val leaderChar = getLeaderChar()
        val leaderIndex = typedKeySequence.indexOfFirst { ks ->
            ks.keyChar == leaderChar ||
                (leaderChar == ' ' && ks.keyCode == KeyEvent.VK_SPACE && ks.modifiers == 0)
        }
        if (leaderIndex < 0) return null
        return typedKeySequence.subList(leaderIndex + 1, typedKeySequence.size)
    }

    private fun applyOverrides(
        entries: Map<String, KeyNode>,
        postLeaderPrefix: String?
    ): Map<String, KeyNode> {
        if (postLeaderPrefix == null) return entries
        val overrides = WhichKeyConfigService.getInstance().overrides
        if (overrides.isEmpty()) return entries

        return entries.mapValues { (key, node) ->
            val override = overrides[postLeaderPrefix + key] ?: return@mapValues node
            applyOverrideToNode(node, override)
        }
    }

    private fun applyOverrideToNode(node: KeyNode, override: OverrideEntry): KeyNode {
        val newDesc = override.description ?: node.description
        val newIcon = IconResolver.resolveIconByKeyword(override.icon) ?: node.icon
        return when (node) {
            is KeyNode.ActionNode -> node.copy(description = newDesc, icon = newIcon)
            is KeyNode.GroupNode -> node.copy(description = newDesc, icon = newIcon)
        }
    }

    private fun tryLeaderLookup(typedKeySequence: List<KeyStroke>): LookupResult? {
        val rootBindings = WhichKeyConfigService.getInstance().rootBindings
        if (rootBindings.isEmpty()) return null

        val postLeaderKeys = findPostLeaderKeys(typedKeySequence) ?: return null

        if (postLeaderKeys.isEmpty()) {
            return LookupResult(emptyList(), rootBindings)
        }

        var current: Map<String, KeyNode> = rootBindings
        val path = mutableListOf<String>()

        for (ks in postLeaderKeys) {
            val key = keyToString(ks)
            val node = current[key] ?: return null
            when (node) {
                is KeyNode.GroupNode -> {
                    path.add(node.description)
                    current = node.children
                }
                is KeyNode.ActionNode -> return null
            }
        }

        return LookupResult(path, current)
    }

    private fun queryAllMappings(
        typedKeySequence: List<KeyStroke>,
        mappingMode: MappingMode
    ): LookupResult? {
        val nestedMappings = mutableMapOf<String, NestedEntry>()

        collectUserMappings(typedKeySequence, mappingMode, nestedMappings)
        collectBuiltinActions(typedKeySequence, mappingMode, nestedMappings)
        collectDuplicableOperator(typedKeySequence, mappingMode, nestedMappings)

        if (nestedMappings.isEmpty()) return null

        val entries = nestedMappings.map { (nextKey, entry) ->
            nextKey to if (entry.isPrefix) {
                KeyNode.GroupNode(
                    key = nextKey,
                    description = entry.description,
                    icon = IconResolver.GROUP_ICON,
                    children = emptyMap()
                )
            } else {
                KeyNode.ActionNode(
                    key = nextKey,
                    description = entry.description,
                    icon = IconResolver.resolveActionIcon(entry.actionId),
                    actionId = entry.actionId ?: ""
                )
            }
        }.toMap()

        return LookupResult(emptyList(), entries)
    }

    @Suppress("DEPRECATION")
    private fun collectUserMappings(
        typedKeySequence: List<KeyStroke>,
        mappingMode: MappingMode,
        nestedMappings: MutableMap<String, NestedEntry>
    ) {
        try {
            val keyMapping = injector.keyGroup.getKeyMapping(mappingMode)
            for (mappingKeyStrokes in keyMapping) {
                if (mappingKeyStrokes.isNotEmpty() && isPlugOrActionKey(mappingKeyStrokes[0])) continue
                if (mappingKeyStrokes.size <= typedKeySequence.size) continue
                if (mappingKeyStrokes.subList(0, typedKeySequence.size) != typedKeySequence) continue

                val nextKeyStroke = mappingKeyStrokes[typedKeySequence.size]
                if (isPlugOrActionKey(nextKeyStroke)) continue

                val nextKey = keyToString(nextKeyStroke)
                if (nextKey in nestedMappings) continue

                val isPrefix = mappingKeyStrokes.size > typedKeySequence.size + 1
                val mappingInfo = keyMapping[mappingKeyStrokes]
                val actionId = mappingInfo?.let { extractActionId(it) }
                val desc = mappingInfo?.let { resolveDescription(actionId, it) } ?: "no description"

                nestedMappings[nextKey] = NestedEntry(
                    isPrefix = isPrefix,
                    description = desc,
                    actionId = actionId,
                    keyStroke = nextKeyStroke
                )
            }
        } catch (_: Exception) {}
    }

    private fun collectBuiltinActions(
        typedKeySequence: List<KeyStroke>,
        mappingMode: MappingMode,
        nestedMappings: MutableMap<String, NestedEntry>
    ) {
        val vimActions = VIM_ACTIONS[mappingMode] ?: return
        for ((actionKeyStrokes, actionId) in vimActions) {
            if (actionKeyStrokes.size <= typedKeySequence.size) continue
            if (actionKeyStrokes.subList(0, typedKeySequence.size) != typedKeySequence) continue
            if (isPlugOrActionKey(actionKeyStrokes[typedKeySequence.size])) continue

            val nextKey = keyToString(actionKeyStrokes[typedKeySequence.size])
            if (nextKey in nestedMappings) continue

            nestedMappings[nextKey] = NestedEntry(
                isPrefix = actionKeyStrokes.size > typedKeySequence.size + 1,
                description = ActionDescriptions.lookup(actionId),
                actionId = actionId,
                keyStroke = actionKeyStrokes[typedKeySequence.size]
            )
        }
    }

    private fun collectDuplicableOperator(
        typedKeySequence: List<KeyStroke>,
        mappingMode: MappingMode,
        nestedMappings: MutableMap<String, NestedEntry>
    ) {
        try {
            val command = injector.keyGroup.getBuiltinCommandsTrie(mappingMode)
                .getData(typedKeySequence) ?: return
            val instance = command.instance
            if (instance !is DuplicableOperatorAction) return

            val dupKeyStroke = KeyStroke.getKeyStroke(instance.duplicateWith)
            val nextKey = keyToString(dupKeyStroke)
            if (nextKey in nestedMappings) return

            val desc = ActionDescriptions.lookup(command.actionId)
            val lineDesc = if (desc.isNotBlank() && desc != "???") "$desc line" else "line"

            nestedMappings[nextKey] = NestedEntry(
                isPrefix = false,
                description = lineDesc,
                actionId = command.actionId,
                keyStroke = dupKeyStroke
            )
        } catch (_: Exception) {}
    }

    private data class NestedEntry(
        val isPrefix: Boolean,
        val description: String,
        val actionId: String?,
        val keyStroke: KeyStroke
    )

    private fun extractActionId(mappingInfo: MappingInfo): String? {
        return when (mappingInfo) {
            is ToActionMappingInfo -> mappingInfo.action
            is ToKeysMappingInfo -> {
                val keysStr = mappingInfo.toKeys.mapNotNull { keyStrokeToChar(it) }.joinToString("")
                val match = ACTION_PATTERN.find(keysStr)
                if (match != null) return match.groupValues[1]

                try {
                    val toKeys = mappingInfo.toKeys.filterNot { isPlugOrActionKey(it) }
                    if (toKeys.isNotEmpty()) {
                        for (mode in listOf(MappingMode.NORMAL, MappingMode.VISUAL, MappingMode.OP_PENDING)) {
                            val command = injector.keyGroup.getBuiltinCommandsTrie(mode).getData(toKeys)
                            if (command != null) return command.actionId
                        }
                    }
                } catch (_: Exception) {}

                null
            }
            else -> null
        }
    }

    private fun resolveDescription(actionId: String?, mappingInfo: MappingInfo?): String {
        actionId?.let { ActionDescriptions.lookup(it).takeIf(String::isNotBlank) }?.let { return it }
        return try {
            when (mappingInfo) {
                is ToActionMappingInfo -> mappingInfo.getPresentableString()
                is ToKeysMappingInfo -> mappingInfo.getPresentableString()
                else -> "???"
            }
        } catch (_: Exception) {
            "???"
        }
    }

    private val ACTION_PATTERN = Regex(":action\\s*(\\S+)")

    // VK_PLUG and VK_ACTION are private in IdeaVim, so we derive them
    private val VK_PLUG = KeyEvent.CHAR_UNDEFINED.code - 1
    private val VK_ACTION = KeyEvent.CHAR_UNDEFINED.code - 2

    private fun isPlugOrActionKey(ks: KeyStroke): Boolean {
        return ks.keyCode == VK_PLUG || ks.keyCode == VK_ACTION
    }

    private fun keyToString(keyStroke: KeyStroke): String {
        return try {
            injector.parser.toKeyNotation(keyStroke)
        } catch (_: Exception) {
            keyStrokeToChar(keyStroke)?.toString() ?: keyStroke.toString()
        }
    }

    fun getLeaderChar(): Char {
        return try {
            val leaderStr = injector.variableService.getGlobalVariableValue("mapleader")?.asString()
            when {
                leaderStr.isNullOrEmpty() -> '\\'
                leaderStr == " " || leaderStr.equals("<Space>", ignoreCase = true) -> ' '
                else -> leaderStr.first()
            }
        } catch (_: Exception) {
            '\\'
        }
    }

    fun keyStrokeToChar(ks: KeyStroke): Char? {
        if (ks.keyChar != KeyEvent.CHAR_UNDEFINED) return ks.keyChar
        return when (ks.keyCode) {
            KeyEvent.VK_SPACE -> ' '
            KeyEvent.VK_TAB -> '\t'
            KeyEvent.VK_ENTER, KeyEvent.VK_ESCAPE -> null
            else -> null
        }
    }
}
