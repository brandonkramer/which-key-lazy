package lazyideavim.whichkeylazy.config

import lazyideavim.whichkeylazy.model.BindingEntry
import lazyideavim.whichkeylazy.model.WhichKeyRoot
import lazyideavim.whichkeylazy.model.WhichKeySettings
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

/**
 * Validates that DefaultConfig.JSON_TEMPLATE is well-formed JSON
 * that deserializes into the expected data model.
 *
 * Catches malformed JSON, missing fields, and structural errors
 * without needing an IDE runtime.
 */
class DefaultConfigTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun `template deserializes without errors`() {
        val root = json.decodeFromString<WhichKeyRoot>(DefaultConfig.JSON_TEMPLATE)
        assertNotNull(root)
    }

    @Test
    fun `default settings have expected values`() {
        val root = json.decodeFromString<WhichKeyRoot>(DefaultConfig.JSON_TEMPLATE)
        assertEquals(200, root.settings.delay)
        assertEquals(5, root.settings.maxColumns)
        assertTrue(root.settings.sortGroupsFirst)
        assertEquals("bottom-right", root.settings.position)
        assertTrue(root.settings.showIcons)
    }

    @Test
    fun `bindings contain expected top-level groups`() {
        val root = json.decodeFromString<WhichKeyRoot>(DefaultConfig.JSON_TEMPLATE)
        val keys = root.bindings.keys
        assertTrue("Should have 'f' group", "f" in keys)
        assertTrue("Should have 'b' group", "b" in keys)
        assertTrue("Should have 'c' group", "c" in keys)
        assertTrue("Should have 's' group", "s" in keys)
        assertTrue("Should have 'g' group", "g" in keys)
        assertTrue("Should have 'w' group", "w" in keys)
        assertTrue("Should have 't' group", "t" in keys)
        assertTrue("Should have 'q' action", "q" in keys)
    }

    @Test
    fun `group entries have children and no actionId`() {
        val root = json.decodeFromString<WhichKeyRoot>(DefaultConfig.JSON_TEMPLATE)
        val fileGroup = root.bindings["f"]!!
        assertNotNull("File group should have children", fileGroup.children)
        assertTrue("File group children should not be empty", fileGroup.children!!.isNotEmpty())
        assertNull("File group should not have actionId", fileGroup.actionId)
    }

    @Test
    fun `action entries have actionId and no children`() {
        val root = json.decodeFromString<WhichKeyRoot>(DefaultConfig.JSON_TEMPLATE)
        val quitAction = root.bindings["q"]!!
        assertNotNull("Quit action should have actionId", quitAction.actionId)
        assertEquals("Exit", quitAction.actionId)
        assertTrue("Quit action should not have children",
            quitAction.children.isNullOrEmpty())
    }

    @Test
    fun `leaf bindings have valid actionIds`() {
        val root = json.decodeFromString<WhichKeyRoot>(DefaultConfig.JSON_TEMPLATE)
        for ((groupKey, group) in root.bindings) {
            if (group.children != null) {
                for ((childKey, child) in group.children!!) {
                    assertFalse(
                        "Binding $groupKey/$childKey should have non-blank actionId",
                        child.actionId.isNullOrBlank()
                    )
                }
            }
        }
    }

    @Test
    fun `overrides section is populated`() {
        val root = json.decodeFromString<WhichKeyRoot>(DefaultConfig.JSON_TEMPLATE)
        assertTrue("Should have overrides", root.overrides.isNotEmpty())
        // Check a few known overrides
        assertNotNull("Should have '|' override", root.overrides["|"])
        assertEquals("Split Right", root.overrides["|"]?.description)
    }

    @Test
    fun `settings default constructor matches expected defaults`() {
        val defaults = WhichKeySettings()
        assertEquals(200, defaults.delay)
        assertEquals(5, defaults.maxColumns)
        assertTrue(defaults.sortGroupsFirst)
        assertEquals("bottom-right", defaults.position)
        assertTrue(defaults.showIcons)
    }
}
