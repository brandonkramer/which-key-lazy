package lazyideavim.whichkeylazy.config

import lazyideavim.whichkeylazy.model.WhichKeySettings
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WhichKeyConfigSaveTest {

    @Test
    fun `updates only placement and gap`() = withTempHome { home ->
        val config = File(home, ".whichkey-lazy.json")
        config.writeText(
            """
            {
              "settings": { "delay": 300, "position": "bottom-right", "showIcons": false },
              "bindings": { "q": { "description": "Quit", "actionId": "Exit" } },
              "overrides": { "|": { "description": "Split" } }
            }
            """.trimIndent()
        )

        val result = WhichKeyConfig.saveSettings(
            WhichKeySettings(placement = "top-right", gapX = 10, gapY = 20)
        )
        assertTrue(result.isSuccess)

        val root = Json.parseToJsonElement(config.readText()).jsonObject
        val settings = root["settings"]!!.jsonObject
        assertEquals("top-right", settings["placement"]!!.jsonPrimitive.content)
        assertEquals(10, settings["gapX"]!!.jsonPrimitive.int)
        assertEquals(20, settings["gapY"]!!.jsonPrimitive.int)
        assertEquals(300, settings["delay"]!!.jsonPrimitive.int)
        assertFalse(settings["showIcons"]!!.jsonPrimitive.boolean)
        assertFalse(settings.containsKey("position"))
        assertEquals(
            "Exit",
            root["bindings"]!!.jsonObject["q"]!!.jsonObject["actionId"]!!.jsonPrimitive.content
        )
        assertEquals(
            "Split",
            root["overrides"]!!.jsonObject["|"]!!.jsonObject["description"]!!.jsonPrimitive.content
        )
    }

    @Test
    fun `creates config from defaults when missing`() = withTempHome { home ->
        val config = File(home, ".whichkey-lazy.json")
        assertFalse(config.exists())

        val result = WhichKeyConfig.saveSettings(
            WhichKeySettings(placement = "top-left", gapX = 1, gapY = 2)
        )
        assertTrue(result.isSuccess)
        assertTrue(config.exists())

        val root = Json.parseToJsonElement(config.readText()).jsonObject
        val settings = root["settings"]!!.jsonObject
        assertEquals("top-left", settings["placement"]!!.jsonPrimitive.content)
        assertEquals(1, settings["gapX"]!!.jsonPrimitive.int)
        assertEquals(2, settings["gapY"]!!.jsonPrimitive.int)
        assertTrue((root["overrides"] as JsonObject).isNotEmpty())
        assertTrue((root["bindings"] as JsonObject).isNotEmpty())
    }

    @Test
    fun `defaults overwrite existing placement`() = withTempHome { home ->
        val config = File(home, ".whichkey-lazy.json")
        config.writeText("""{ "settings": { "placement": "top-right", "gapX": 9, "gapY": 9 } }""")

        assertTrue(WhichKeyConfig.saveSettings(WhichKeySettings()).isSuccess)

        val settings = Json.parseToJsonElement(config.readText()).jsonObject["settings"]!!.jsonObject
        assertEquals("bottom-center", settings["placement"]!!.jsonPrimitive.content)
        assertEquals(0, settings["gapX"]!!.jsonPrimitive.int)
        assertEquals(0, settings["gapY"]!!.jsonPrimitive.int)
    }

    private fun withTempHome(block: (File) -> Unit) {
        val previousHome = System.getProperty("user.home")
        val tempHome = Files.createTempDirectory("whichkey-settings-test").toFile()
        System.setProperty("user.home", tempHome.absolutePath)
        try {
            block(tempHome)
        } finally {
            System.setProperty("user.home", previousHome)
            tempHome.deleteRecursively()
        }
    }
}
