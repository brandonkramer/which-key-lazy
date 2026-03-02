package lazyideavim.whichkeylazy.ideavim

import lazyideavim.whichkeylazy.ActionDescriptions
import lazyideavim.whichkeylazy.model.KeyNode
import lazyideavim.whichkeylazy.ui.IconResolver
import javax.swing.Icon

/**
 * Converts flat IdeaVim mappings into a hierarchical KeyNode tree.
 *
 * For example, mappings: ff->GotoFile, fr->RecentFiles, bd->CloseContent
 * Produces:
 *   f (GroupNode) -> { f: ActionNode(GotoFile), r: ActionNode(RecentFiles) }
 *   b (GroupNode) -> { d: ActionNode(CloseContent) }
 */
object IdeaVimTreeBuilder {

    fun buildTree(parseResult: IdeaVimParseResult): Map<String, KeyNode> {
        val normalMappings = parseResult.mappings.filter { mapping ->
            mapping.mode.contains('n') || mapping.mode.isEmpty()
        }
        if (normalMappings.isEmpty()) return emptyMap()

        val root = TrieNode()
        for (mapping in normalMappings) {
            var node = root
            for (ch in mapping.keySequence) {
                val key = charToNotation(ch)
                node = node.children.getOrPut(key) { TrieNode() }
            }
            node.actionId = mapping.actionId
            node.icon = IconResolver.resolveActionIcon(mapping.actionId)
        }

        return trieToKeyNodes(root, parseResult.descriptions, prefix = "")
    }

    private fun trieToKeyNodes(
        node: TrieNode,
        descriptions: Map<String, String>,
        prefix: String
    ): Map<String, KeyNode> {
        return node.children.map { (key, child) ->
            val fullKey = prefix + key
            val keyNode = if (child.children.isNotEmpty()) {
                val groupDesc = resolveGroupDescription(fullKey, descriptions)
                KeyNode.GroupNode(
                    key = key,
                    description = groupDesc,
                    icon = IconResolver.resolveGroupIcon(groupDesc),
                    children = trieToKeyNodes(child, descriptions, fullKey)
                )
            } else if (child.actionId != null) {
                KeyNode.ActionNode(
                    key = key,
                    description = resolveActionDescription(fullKey, child.actionId!!, descriptions),
                    icon = child.icon,
                    actionId = child.actionId!!
                )
            } else {
                KeyNode.ActionNode(
                    key = key,
                    description = "???",
                    icon = null,
                    actionId = ""
                )
            }
            key to keyNode
        }.toMap()
    }

    /**
     * Priority: g:WhichKeyDesc_ > ActionManager presentation text > humanized action ID.
     */
    private fun resolveActionDescription(
        keyPath: String,
        actionId: String,
        whichKeyDescs: Map<String, String>
    ): String {
        whichKeyDescs[keyPath]?.let { return it }
        return ActionDescriptions.lookup(actionId)
    }

    private fun resolveGroupDescription(
        keyPath: String,
        whichKeyDescs: Map<String, String>
    ): String {
        whichKeyDescs[keyPath]?.let { return it }
        DefaultGroupNames.getDefault(keyPath)?.let { return it }
        val lastKey = keyPath.takeLastWhile { it != '>' && it != '<' }
            .ifEmpty { keyPath.removeSurrounding("<", ">") }
        return "+${lastKey.ifEmpty { "?" }}"
    }

    private fun charToNotation(ch: Char): String {
        return when (ch) {
            ' ' -> " "
            '\t' -> "<Tab>"
            '\u0008' -> "<BS>"
            '\u007f' -> "<Del>"
            else -> ch.toString()
        }
    }

    private class TrieNode {
        val children: MutableMap<String, TrieNode> = mutableMapOf()
        var actionId: String? = null
        var icon: Icon? = null
    }
}
