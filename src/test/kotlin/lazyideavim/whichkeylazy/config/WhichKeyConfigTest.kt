package lazyideavim.whichkeylazy.config

import lazyideavim.whichkeylazy.model.*
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for JSON config parsing and binding conversion logic.
 *
 * Tests the serialization layer directly (WhichKeyRoot deserialization)
 * rather than WhichKeyConfig.load() which depends on file I/O and IconResolver.
 */
class WhichKeyConfigTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun `parses minimal valid config`() {
        val input = """
            {
              "settings": {},
              "bindings": {},
              "overrides": {}
            }
        """.trimIndent()
        val root = json.decodeFromString<WhichKeyRoot>(input)
        assertEquals(WhichKeySettings(), root.settings)
        assertTrue(root.bindings.isEmpty())
        assertTrue(root.overrides.isEmpty())
    }

    @Test
    fun `parses config with custom settings`() {
        val input = """
            {
              "settings": {
                "delay": 500,
                "maxColumns": 3,
                "sortGroupsFirst": false,
                "position": "center",
                "showIcons": false
              }
            }
        """.trimIndent()
        val root = json.decodeFromString<WhichKeyRoot>(input)
        assertEquals(500, root.settings.delay)
        assertEquals(3, root.settings.maxColumns)
        assertFalse(root.settings.sortGroupsFirst)
        assertEquals("center", root.settings.position)
        assertFalse(root.settings.showIcons)
    }

    @Test
    fun `parses action binding entry`() {
        val input = """
            {
              "bindings": {
                "q": { "description": "Quit", "actionId": "Exit" }
              }
            }
        """.trimIndent()
        val root = json.decodeFromString<WhichKeyRoot>(input)
        val entry = root.bindings["q"]!!
        assertEquals("Quit", entry.description)
        assertEquals("Exit", entry.actionId)
        assertNull(entry.children)
    }

    @Test
    fun `parses group binding with children`() {
        val input = """
            {
              "bindings": {
                "f": {
                  "description": "File",
                  "children": {
                    "f": { "description": "Find File", "actionId": "GotoFile" },
                    "r": { "description": "Recent Files", "actionId": "RecentFiles" }
                  }
                }
              }
            }
        """.trimIndent()
        val root = json.decodeFromString<WhichKeyRoot>(input)
        val group = root.bindings["f"]!!
        assertEquals("File", group.description)
        assertNull(group.actionId)
        assertEquals(2, group.children!!.size)
        assertEquals("GotoFile", group.children!!["f"]!!.actionId)
        assertEquals("RecentFiles", group.children!!["r"]!!.actionId)
    }

    @Test
    fun `parses override entries`() {
        val input = """
            {
              "overrides": {
                "|": { "icon": "windows", "description": "Split Right" },
                "K": { "description": "Help" },
                "l": { "icon": "AllIcons.Nodes.Plugin" }
              }
            }
        """.trimIndent()
        val root = json.decodeFromString<WhichKeyRoot>(input)
        assertEquals(3, root.overrides.size)

        val pipe = root.overrides["|"]!!
        assertEquals("windows", pipe.icon)
        assertEquals("Split Right", pipe.description)

        val k = root.overrides["K"]!!
        assertNull(k.icon)
        assertEquals("Help", k.description)

        val l = root.overrides["l"]!!
        assertEquals("AllIcons.Nodes.Plugin", l.icon)
        assertNull(l.description)
    }

    @Test
    fun `ignores unknown keys in JSON`() {
        val input = """
            {
              "settings": { "delay": 100, "unknownField": true },
              "bindings": {},
              "futureSection": { "foo": "bar" }
            }
        """.trimIndent()
        val root = json.decodeFromString<WhichKeyRoot>(input)
        assertEquals(100, root.settings.delay)
    }

    @Test
    fun `empty JSON object uses all defaults`() {
        val input = "{}"
        val root = json.decodeFromString<WhichKeyRoot>(input)
        assertEquals(WhichKeySettings(), root.settings)
        assertTrue(root.bindings.isEmpty())
        assertTrue(root.overrides.isEmpty())
    }

    @Test
    fun `nested group two levels deep`() {
        val input = """
            {
              "bindings": {
                "s": {
                  "description": "Search",
                  "children": {
                    "n": {
                      "description": "Noice",
                      "children": {
                        "d": { "description": "Dismiss", "actionId": "ClearAllNotifications" }
                      }
                    }
                  }
                }
              }
            }
        """.trimIndent()
        val root = json.decodeFromString<WhichKeyRoot>(input)
        val search = root.bindings["s"]!!
        val noice = search.children!!["n"]!!
        assertEquals("Noice", noice.description)
        assertNotNull(noice.children)
        assertEquals("ClearAllNotifications", noice.children!!["d"]!!.actionId)
    }

    @Test
    fun `binding with icon field`() {
        val input = """
            {
              "bindings": {
                "g": {
                  "description": "+git",
                  "icon": "git",
                  "children": {
                    "s": { "description": "Status", "actionId": "Vcs.Show.Local.Changes" }
                  }
                }
              }
            }
        """.trimIndent()
        val root = json.decodeFromString<WhichKeyRoot>(input)
        val gitGroup = root.bindings["g"]!!
        assertEquals("git", gitGroup.icon)
    }
}
