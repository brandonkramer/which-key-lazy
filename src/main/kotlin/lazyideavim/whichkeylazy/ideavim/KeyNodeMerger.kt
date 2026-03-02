package lazyideavim.whichkeylazy.ideavim

import lazyideavim.whichkeylazy.model.KeyNode

/**
 * Merges two KeyNode trees. The [overlay] takes precedence over [base].
 *
 * Merge rules:
 * - If overlay has a key not in base → add it
 * - If both have GroupNodes → recursively merge children (overlay description wins)
 * - Any other conflict → overlay wins entirely
 */
object KeyNodeMerger {

    fun merge(
        base: Map<String, KeyNode>,
        overlay: Map<String, KeyNode>
    ): Map<String, KeyNode> {
        if (overlay.isEmpty()) return base
        if (base.isEmpty()) return overlay

        val result = base.toMutableMap()

        for ((key, overlayNode) in overlay) {
            val baseNode = result[key]
            result[key] = when {
                baseNode == null -> overlayNode

                baseNode is KeyNode.GroupNode && overlayNode is KeyNode.GroupNode -> {
                    KeyNode.GroupNode(
                        key = key,
                        description = overlayNode.description,
                        icon = overlayNode.icon ?: baseNode.icon,
                        children = merge(baseNode.children, overlayNode.children)
                    )
                }

                // Overlay wins in all other cases (type mismatch, action vs action, etc.)
                else -> overlayNode
            }
        }

        return result
    }
}
