package lazyideavim.whichkeylazy.ideavim

import lazyideavim.whichkeylazy.ActionDescriptions
import lazyideavim.whichkeylazy.dispatch.MappingLookup
import lazyideavim.whichkeylazy.model.KeyNode
import lazyideavim.whichkeylazy.ui.IconResolver
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.key.MappingInfo
import com.maddyhome.idea.vim.key.ToActionMappingInfo
import com.maddyhome.idea.vim.key.ToKeysMappingInfo
import java.awt.event.KeyEvent
import javax.swing.Icon
import javax.swing.KeyStroke

/**
 * Reads IdeaVim keybindings at runtime via the IdeaVim injector API.
 *
 * Preferred over .ideavimrc file parsing because it captures all mappings
 * (including `source`d files and recursive mappings) and reflects actual runtime state.
 */
object IdeaVimApiReader {

    fun readMappings(): Map<String, KeyNode> {
        return try {
            val leaderChar = MappingLookup.getLeaderChar()
            val leaderStroke = if (leaderChar == ' ') {
                KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0)
            } else {
                KeyStroke.getKeyStroke(leaderChar)
            }
            val descriptions = extractWhichKeyDescriptions(leaderChar)
            val mappings = extractLeaderMappings(leaderStroke)

            if (mappings.isEmpty()) return emptyMap()

            buildTree(mappings, descriptions)
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun isAvailable(): Boolean {
        return try {
            injector.keyGroup
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Extract g:WhichKeyDesc_ variables from IdeaVim's variable service.
     * Returns a map of key suffix (e.g. "ff") to description text.
     */
    private fun extractWhichKeyDescriptions(leaderChar: Char): Map<String, String> {
        val descriptions = mutableMapOf<String, String>()
        try {
            val descRegex = Regex("([^ \\t]+)[ \\t]*(.*)")
            for ((name, value) in injector.variableService.getGlobalVariables()) {
                if (!name.startsWith("WhichKeyDesc_")) continue
                val match = descRegex.find(value.asString()) ?: continue
                val keySequence = match.groupValues[1]
                val description = match.groupValues[2].trim()

                val normalized = keySequence
                    .replace("<leader>", leaderChar.toString(), ignoreCase = true)
                    .replace("<Space>", " ", ignoreCase = true)

                val leaderIdx = normalized.indexOf(leaderChar)
                if (leaderIdx >= 0 && description.isNotEmpty()) {
                    val suffix = normalized.substring(leaderIdx + 1)
                    if (suffix.isNotEmpty()) {
                        descriptions[suffix] = description
                    }
                }
            }
        } catch (_: Exception) { }
        return descriptions
    }

    private data class FlatMapping(
        val keySequence: List<String>,
        val actionId: String?,
        val description: String,
        val icon: Icon?
    )

    /**
     * Extract all normal-mode mappings that start with the leader key.
     * Tries the modern KeyMapping.getAll(prefix) API first (IdeaVim 2.17.0+),
     * falling back to iterating all mappings for older versions.
     */
    private fun extractLeaderMappings(leaderStroke: KeyStroke): List<FlatMapping> {
        val keyMapping = injector.keyGroup.getKeyMapping(MappingMode.NORMAL)
        val leaderPrefix = listOf(leaderStroke)

        return try {
            extractViaGetAll(keyMapping, leaderPrefix)
        } catch (_: NoSuchMethodError) {
            extractViaIterable(keyMapping, leaderPrefix)
        }
    }

    private fun extractViaGetAll(
        keyMapping: com.maddyhome.idea.vim.key.KeyMapping,
        leaderPrefix: List<KeyStroke>
    ): List<FlatMapping> {
        return keyMapping.getAll(leaderPrefix).mapNotNull { entry ->
            processMapping(entry.getPath(), entry.mappingInfo)
        }.toList()
    }

    @Suppress("DEPRECATION")
    private fun extractViaIterable(
        keyMapping: com.maddyhome.idea.vim.key.KeyMapping,
        leaderPrefix: List<KeyStroke>
    ): List<FlatMapping> {
        return buildList {
            for (keyStrokes in keyMapping) {
                if (keyStrokes.size < leaderPrefix.size) continue
                if (keyStrokes.subList(0, leaderPrefix.size) != leaderPrefix) continue

                val mappingInfo = keyMapping[keyStrokes] ?: continue
                processMapping(keyStrokes, mappingInfo)?.let { add(it) }
            }
        }
    }

    private fun processMapping(keyStrokes: List<KeyStroke>, mappingInfo: MappingInfo): FlatMapping? {
        if (keyStrokes.size <= 1) return null

        val postLeader = keyStrokes.subList(1, keyStrokes.size)
        val keyNotations = postLeader.map { keyStrokeToNotation(it) }
        if (keyNotations.any { it == UNMAPPABLE }) return null
        if (keyNotations.any { it.startsWith("<Plug>") || it.startsWith("<Action>") }) return null

        val (actionId, desc) = when (mappingInfo) {
            is ToActionMappingInfo -> mappingInfo.action to ActionDescriptions.lookup(mappingInfo.action)
            is ToKeysMappingInfo -> resolveToKeysMapping(mappingInfo)
            else -> null to mappingInfo.getPresentableString()
        }

        return FlatMapping(
            keySequence = keyNotations,
            actionId = actionId,
            description = desc,
            icon = IconResolver.resolveActionIcon(actionId)
        )
    }

    /**
     * Resolve a ToKeysMappingInfo to an (actionId, description) pair.
     * Tries parsing `:action ActionId<CR>`, then looks up the target keys
     * in IdeaVim's builtin commands trie.
     */
    private fun resolveToKeysMapping(info: ToKeysMappingInfo): Pair<String?, String> {
        val toKeys = info.toKeys

        val keysStr = toKeys.mapNotNull { MappingLookup.keyStrokeToChar(it) }.joinToString("")
        val actionMatch = ACTION_PATTERN.find(keysStr)
        if (actionMatch != null) {
            val actionId = actionMatch.groupValues[1]
            return actionId to ActionDescriptions.lookup(actionId)
        }

        try {
            val filteredKeys = toKeys.filterNot { it.keyCode == VK_PLUG || it.keyCode == VK_ACTION }
            if (filteredKeys.isNotEmpty()) {
                for (mode in listOf(MappingMode.NORMAL, MappingMode.VISUAL, MappingMode.OP_PENDING)) {
                    val command = injector.keyGroup.getBuiltinCommandsTrie(mode).getData(filteredKeys)
                    if (command != null) {
                        return command.actionId to ActionDescriptions.lookup(command.actionId)
                    }
                }
            }
        } catch (_: Exception) {}

        return null to info.getPresentableString()
    }

    private val ACTION_PATTERN = Regex(":action\\s*(\\S+)")
    private const val UNMAPPABLE = "\u0000"
    private val VK_PLUG = KeyEvent.CHAR_UNDEFINED.code - 1
    private val VK_ACTION = KeyEvent.CHAR_UNDEFINED.code - 2

    private fun keyStrokeToNotation(ks: KeyStroke): String {
        if (ks.keyCode == KeyEvent.VK_ENTER || ks.keyCode == KeyEvent.VK_ESCAPE) return UNMAPPABLE
        if (ks.keyChar == '\n' || ks.keyChar == '\r' || ks.keyChar == '\u001b') return UNMAPPABLE
        return try {
            injector.parser.toKeyNotation(ks)
        } catch (_: Exception) {
            UNMAPPABLE
        }
    }

    private fun buildTree(
        mappings: List<FlatMapping>,
        descriptions: Map<String, String>
    ): Map<String, KeyNode> {
        val root = TrieNode()
        for (mapping in mappings) {
            var node = root
            for (notation in mapping.keySequence) {
                node = node.children.getOrPut(notation) { TrieNode() }
            }
            node.actionId = mapping.actionId
            node.defaultDescription = mapping.description
            node.icon = mapping.icon
        }
        return trieToKeyNodes(root, descriptions, "")
    }

    private fun trieToKeyNodes(
        node: TrieNode,
        descriptions: Map<String, String>,
        prefix: String
    ): Map<String, KeyNode> {
        return node.children.map { (notation, child) ->
            val fullKey = prefix + notation
            val keyNode = if (child.children.isNotEmpty()) {
                val groupDesc = descriptions[fullKey]
                    ?: child.defaultDescription.takeIf { it.isNotBlank() }
                    ?: DefaultGroupNames.getDefault(fullKey)
                    ?: "+${notation.removeSurrounding("<", ">").lowercase()}"
                KeyNode.GroupNode(
                    key = notation,
                    description = groupDesc,
                    icon = IconResolver.resolveGroupIcon(groupDesc),
                    children = trieToKeyNodes(child, descriptions, fullKey)
                )
            } else {
                val desc = descriptions[fullKey]
                    ?: child.defaultDescription.takeIf { it.isNotBlank() }
                    ?: child.actionId?.let { ActionDescriptions.lookup(it) }
                    ?: "???"
                KeyNode.ActionNode(
                    key = notation,
                    description = desc,
                    icon = child.icon,
                    actionId = child.actionId ?: ""
                )
            }
            notation to keyNode
        }.toMap()
    }

    private class TrieNode {
        val children: MutableMap<String, TrieNode> = mutableMapOf()
        var actionId: String? = null
        var defaultDescription: String = ""
        var icon: Icon? = null
    }
}
