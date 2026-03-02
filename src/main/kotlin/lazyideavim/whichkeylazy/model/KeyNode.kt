package lazyideavim.whichkeylazy.model

import javax.swing.Icon

sealed class KeyNode {
    abstract val key: String
    abstract val description: String
    abstract val icon: Icon?

    data class GroupNode(
        override val key: String,
        override val description: String,
        override val icon: Icon?,
        val children: Map<String, KeyNode>
    ) : KeyNode()

    data class ActionNode(
        override val key: String,
        override val description: String,
        override val icon: Icon?,
        val actionId: String
    ) : KeyNode()
}
