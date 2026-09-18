package lazyideavim.whichkeylazy.dispatch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MappingLookupTest {
    @Test
    fun `extracts action element from IdeaVim presentation`() {
        assertEquals("Inline", extractActionIdFromText("<Action>(Inline)"))
    }

    @Test
    fun `extracts command action without trailing enter token`() {
        assertEquals("ShowSettings", extractActionIdFromText(":action ShowSettings<CR>"))
    }

    @Test
    fun `ignores regular Vim commands`() {
        assertNull(extractActionIdFromText(":setlocal wrap!<CR>"))
    }
}
