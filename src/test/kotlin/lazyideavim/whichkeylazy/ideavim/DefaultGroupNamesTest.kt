package lazyideavim.whichkeylazy.ideavim

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for DefaultGroupNames — the LazyVim default group name lookup table.
 */
class DefaultGroupNamesTest {

    @Test
    fun `known single-char keys return expected descriptions`() {
        assertEquals("+ai", DefaultGroupNames.getDefault("a"))
        assertEquals("+buffer", DefaultGroupNames.getDefault("b"))
        assertEquals("+code", DefaultGroupNames.getDefault("c"))
        assertEquals("+debug", DefaultGroupNames.getDefault("d"))
        assertEquals("+file/find", DefaultGroupNames.getDefault("f"))
        assertEquals("+git", DefaultGroupNames.getDefault("g"))
        assertEquals("+notifications", DefaultGroupNames.getDefault("n"))
        assertEquals("+overseer", DefaultGroupNames.getDefault("o"))
        assertEquals("+quit/session", DefaultGroupNames.getDefault("q"))
        assertEquals("+refactor", DefaultGroupNames.getDefault("r"))
        assertEquals("+search", DefaultGroupNames.getDefault("s"))
        assertEquals("+test", DefaultGroupNames.getDefault("t"))
        assertEquals("+ui", DefaultGroupNames.getDefault("u"))
        assertEquals("+windows", DefaultGroupNames.getDefault("w"))
        assertEquals("+diagnostics/quickfix", DefaultGroupNames.getDefault("x"))
    }

    @Test
    fun `known multi-char key returns expected description`() {
        assertEquals("+noice", DefaultGroupNames.getDefault("sn"))
    }

    @Test
    fun `unknown key returns null`() {
        assertNull(DefaultGroupNames.getDefault("z"))
        assertNull(DefaultGroupNames.getDefault("ZZ"))
        assertNull(DefaultGroupNames.getDefault(""))
    }

    @Test
    fun `keys are case-sensitive`() {
        // Only lowercase keys are in the map
        assertNull(DefaultGroupNames.getDefault("A"))
        assertNull(DefaultGroupNames.getDefault("F"))
        assertNotNull(DefaultGroupNames.getDefault("a"))
        assertNotNull(DefaultGroupNames.getDefault("f"))
    }
}
