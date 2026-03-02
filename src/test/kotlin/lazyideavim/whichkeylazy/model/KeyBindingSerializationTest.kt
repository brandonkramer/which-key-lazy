package lazyideavim.whichkeylazy.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for data model serialization round-trips.
 * Ensures JSON encoding/decoding works correctly for all config types.
 */
class KeyBindingSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    // --- WhichKeySettings ---

    @Test
    fun `WhichKeySettings round-trip with defaults`() {
        val original = WhichKeySettings()
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<WhichKeySettings>(encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun `WhichKeySettings round-trip with custom values`() {
        val original = WhichKeySettings(
            delay = 500,
            maxColumns = 3,
            sortGroupsFirst = false,
            position = "center",
            showIcons = false
        )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<WhichKeySettings>(encoded)
        assertEquals(original, decoded)
    }

    // --- OverrideEntry ---

    @Test
    fun `OverrideEntry with both fields`() {
        val original = OverrideEntry(description = "Split Right", icon = "windows")
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<OverrideEntry>(encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun `OverrideEntry with only description`() {
        val original = OverrideEntry(description = "Help", icon = null)
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<OverrideEntry>(encoded)
        assertEquals("Help", decoded.description)
        assertNull(decoded.icon)
    }

    @Test
    fun `OverrideEntry with only icon`() {
        val original = OverrideEntry(description = null, icon = "git")
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<OverrideEntry>(encoded)
        assertNull(decoded.description)
        assertEquals("git", decoded.icon)
    }

    // --- BindingEntry ---

    @Test
    fun `BindingEntry action leaf`() {
        val original = BindingEntry(
            description = "Find File",
            actionId = "GotoFile"
        )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<BindingEntry>(encoded)
        assertEquals("Find File", decoded.description)
        assertEquals("GotoFile", decoded.actionId)
        assertNull(decoded.children)
        assertNull(decoded.icon)
    }

    @Test
    fun `BindingEntry group with children`() {
        val children = mapOf(
            "f" to BindingEntry(description = "Find File", actionId = "GotoFile"),
            "r" to BindingEntry(description = "Recent", actionId = "RecentFiles")
        )
        val original = BindingEntry(
            description = "File",
            children = children
        )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<BindingEntry>(encoded)
        assertEquals("File", decoded.description)
        assertNull(decoded.actionId)
        assertEquals(2, decoded.children!!.size)
        assertEquals("GotoFile", decoded.children!!["f"]!!.actionId)
    }

    @Test
    fun `BindingEntry with icon field`() {
        val original = BindingEntry(
            description = "+git",
            icon = "git",
            children = mapOf(
                "s" to BindingEntry(description = "Status", actionId = "Vcs.Show.Local.Changes")
            )
        )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<BindingEntry>(encoded)
        assertEquals("git", decoded.icon)
    }

    // --- WhichKeyRoot ---

    @Test
    fun `WhichKeyRoot full round-trip`() {
        val original = WhichKeyRoot(
            settings = WhichKeySettings(delay = 300),
            bindings = mapOf(
                "f" to BindingEntry(
                    description = "File",
                    children = mapOf(
                        "f" to BindingEntry(description = "Find", actionId = "GotoFile")
                    )
                ),
                "q" to BindingEntry(description = "Quit", actionId = "Exit")
            ),
            overrides = mapOf(
                "|" to OverrideEntry(description = "Split Right", icon = "windows")
            )
        )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<WhichKeyRoot>(encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun `WhichKeyRoot empty defaults`() {
        val original = WhichKeyRoot()
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<WhichKeyRoot>(encoded)
        assertEquals(WhichKeySettings(), decoded.settings)
        assertTrue(decoded.bindings.isEmpty())
        assertTrue(decoded.overrides.isEmpty())
    }
}
