package lazyideavim.whichkeylazy.model

import kotlinx.serialization.Serializable

@Serializable
data class WhichKeySettings(
    val delay: Int = 200,
    val maxColumns: Int = 5,
    val sortGroupsFirst: Boolean = true,
    val position: String = "bottom-right",
    val showIcons: Boolean = true
)

@Serializable
data class WhichKeyRoot(
    val settings: WhichKeySettings = WhichKeySettings(),
    val bindings: Map<String, BindingEntry> = emptyMap(),
    val overrides: Map<String, OverrideEntry> = emptyMap()
)

/**
 * Per-key override for icon and/or description.
 * Keys are post-leader sequences (e.g., "|" for <Space>|, "fe" for <Space>fe).
 * Icon values match keywords from IconResolver (e.g., "windows", "git", "search").
 */
@Serializable
data class OverrideEntry(
    val description: String? = null,
    val icon: String? = null
)

@Serializable
data class BindingEntry(
    val description: String,
    val icon: String? = null,
    val actionId: String? = null,
    val children: Map<String, BindingEntry>? = null
)
