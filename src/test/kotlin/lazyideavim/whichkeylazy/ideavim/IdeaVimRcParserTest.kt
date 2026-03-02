package lazyideavim.whichkeylazy.ideavim

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Tests for IdeaVimRcParser — parsing .ideavimrc content
 * into leader key, mappings, and WhichKeyDesc descriptions.
 *
 * Uses temp files since parse() takes a File parameter.
 */
class IdeaVimRcParserTest {

    @get:Rule
    val tempDir = TemporaryFolder()

    private fun parseContent(content: String): IdeaVimParseResult {
        val file = tempDir.newFile(".ideavimrc")
        file.writeText(content)
        return IdeaVimRcParser.parse(file)
    }

    // --- Leader Key ---

    @Test
    fun `parses space leader`() {
        val result = parseContent("""
            let mapleader=" "
            nmap <leader>ff <Action>(GotoFile)
        """.trimIndent())
        assertEquals(' ', result.leader)
    }

    @Test
    fun `parses backslash leader`() {
        val result = parseContent("""
            let mapleader="\"
            nmap <leader>ff <Action>(GotoFile)
        """.trimIndent())
        assertEquals('\\', result.leader)
    }

    @Test
    fun `defaults to backslash when no leader set`() {
        val result = parseContent("""
            nmap <leader>ff <Action>(GotoFile)
        """.trimIndent())
        assertEquals('\\', result.leader)
    }

    @Test
    fun `leader defined after mappings still applies`() {
        // Two-pass parsing should handle this
        val result = parseContent("""
            nmap <leader>ff <Action>(GotoFile)
            let mapleader=" "
        """.trimIndent())
        assertEquals(' ', result.leader)
        assertEquals(1, result.mappings.size)
    }

    // --- Action Mappings ---

    @Test
    fun `parses Action syntax`() {
        val result = parseContent("""
            let mapleader=" "
            nmap <leader>ff <Action>(GotoFile)
        """.trimIndent())
        assertEquals(1, result.mappings.size)
        assertEquals("ff", result.mappings[0].keySequence)
        assertEquals("GotoFile", result.mappings[0].actionId)
    }

    @Test
    fun `parses colon action syntax`() {
        val result = parseContent("""
            let mapleader=" "
            nnoremap <leader>/ :action FindInPath<CR>
        """.trimIndent())
        assertEquals(1, result.mappings.size)
        assertEquals("/", result.mappings[0].keySequence)
        assertEquals("FindInPath", result.mappings[0].actionId)
    }

    @Test
    fun `parses nmap as normal mode`() {
        val result = parseContent("""
            let mapleader=" "
            nmap <leader>q <Action>(Exit)
        """.trimIndent())
        assertEquals("n", result.mappings[0].mode)
    }

    @Test
    fun `parses bare map as normal and visual`() {
        val result = parseContent("""
            let mapleader=" "
            map <leader>q <Action>(Exit)
        """.trimIndent())
        assertEquals("nv", result.mappings[0].mode)
    }

    @Test
    fun `parses vmap as visual mode`() {
        val result = parseContent("""
            let mapleader=" "
            vmap <leader>cf <Action>(Format)
        """.trimIndent())
        assertEquals("v", result.mappings[0].mode)
    }

    @Test
    fun `parses xmap as visual mode`() {
        val result = parseContent("""
            let mapleader=" "
            xmap <leader>cf <Action>(Format)
        """.trimIndent())
        assertEquals("v", result.mappings[0].mode)
    }

    @Test
    fun `distinguishes map from noremap`() {
        val result = parseContent("""
            let mapleader=" "
            nmap <leader>a <Action>(GotoFile)
            nnoremap <leader>b <Action>(RecentFiles)
        """.trimIndent())
        assertTrue("nmap should be recursive", result.mappings[0].isRecursive)
        assertFalse("nnoremap should not be recursive", result.mappings[1].isRecursive)
    }

    @Test
    fun `skips non-leader mappings`() {
        val result = parseContent("""
            let mapleader=" "
            nmap <C-h> <C-w>h
            nmap gd <Action>(GotoDeclaration)
            nmap <leader>ff <Action>(GotoFile)
        """.trimIndent())
        // Only leader-prefixed action mappings should be included
        assertEquals(1, result.mappings.size)
        assertEquals("ff", result.mappings[0].keySequence)
    }

    @Test
    fun `skips non-action mappings`() {
        val result = parseContent("""
            let mapleader=" "
            nmap <leader>bb <C-^>
            nmap <leader>ff <Action>(GotoFile)
        """.trimIndent())
        // <C-^> is not an action mapping, should be skipped
        assertEquals(1, result.mappings.size)
        assertEquals("GotoFile", result.mappings[0].actionId)
    }

    @Test
    fun `ignores comments and blank lines`() {
        val result = parseContent("""
            " This is a comment
            let mapleader=" "

            " Another comment
            nmap <leader>ff <Action>(GotoFile)

        """.trimIndent())
        assertEquals(1, result.mappings.size)
    }

    @Test
    fun `parses multiple mappings`() {
        val result = parseContent("""
            let mapleader=" "
            nmap <leader>ff <Action>(GotoFile)
            nmap <leader>fr <Action>(RecentFiles)
            nmap <leader>bd <Action>(CloseContent)
            nmap <leader>gg <Action>(ActivateCommitToolWindow)
        """.trimIndent())
        assertEquals(4, result.mappings.size)
    }

    @Test
    fun `strips CR from colon action pattern`() {
        val result = parseContent("""
            let mapleader=" "
            nnoremap <leader>/ :action FindInPath<CR>
        """.trimIndent())
        assertEquals("FindInPath", result.mappings[0].actionId)
    }

    @Test
    fun `handles Space token in LHS`() {
        val result = parseContent("""
            let mapleader=" "
            nmap <Space>ff <Action>(GotoFile)
        """.trimIndent())
        assertEquals(1, result.mappings.size)
        assertEquals("ff", result.mappings[0].keySequence)
    }

    // --- Modifier Key Filtering ---

    @Test
    fun `skips mappings with modifier keys in sequence`() {
        val result = parseContent("""
            let mapleader=" "
            nmap <leader><C-n> <Action>(SelectAll)
            nmap <leader>ff <Action>(GotoFile)
        """.trimIndent())
        // <C-n> contains a modifier key, should be skipped
        assertEquals(1, result.mappings.size)
        assertEquals("ff", result.mappings[0].keySequence)
    }

    // --- WhichKeyDesc Descriptions ---

    @Test
    fun `parses WhichKeyDesc variables`() {
        val result = parseContent("""
            let mapleader=" "
            let g:WhichKeyDesc_f = "<leader>f +file/find"
            nmap <leader>ff <Action>(GotoFile)
        """.trimIndent())
        assertEquals("+file/find", result.descriptions["f"])
    }

    @Test
    fun `parses multiple WhichKeyDesc variables`() {
        val result = parseContent("""
            let mapleader=" "
            let g:WhichKeyDesc_f = "<leader>f +file/find"
            let g:WhichKeyDesc_g = "<leader>g +git"
            let g:WhichKeyDesc_b = "<leader>b +buffer"
        """.trimIndent())
        assertEquals(3, result.descriptions.size)
        assertEquals("+file/find", result.descriptions["f"])
        assertEquals("+git", result.descriptions["g"])
        assertEquals("+buffer", result.descriptions["b"])
    }

    // --- Source Directive ---

    @Test
    fun `follows source directive`() {
        val mainFile = tempDir.newFile(".ideavimrc")
        val sourcedFile = tempDir.newFile("extra.vim")
        sourcedFile.writeText("""
            nmap <leader>ff <Action>(GotoFile)
        """.trimIndent())
        mainFile.writeText("""
            let mapleader=" "
            source ${sourcedFile.absolutePath}
        """.trimIndent())

        val result = IdeaVimRcParser.parse(mainFile)
        assertEquals(1, result.mappings.size)
        assertEquals("GotoFile", result.mappings[0].actionId)
    }

    // --- Edge Cases ---

    @Test
    fun `nonexistent file returns empty result`() {
        val file = tempDir.root.resolve("nonexistent.vim")
        val result = IdeaVimRcParser.parse(file)
        assertEquals('\\', result.leader)
        assertTrue(result.mappings.isEmpty())
        assertTrue(result.descriptions.isEmpty())
    }

    @Test
    fun `empty file returns default result`() {
        val result = parseContent("")
        assertEquals('\\', result.leader)
        assertTrue(result.mappings.isEmpty())
    }

    @Test
    fun `handles Tab special key in mapping`() {
        val result = parseContent("""
            let mapleader=" "
            nmap <leader><tab>l <Action>(GoToLastTab)
        """.trimIndent())
        // <tab> gets converted to \t char, remaining <...> tokens stripped
        // So the key sequence should contain the tab representation
        assertEquals(1, result.mappings.size)
        assertEquals("GoToLastTab", result.mappings[0].actionId)
    }
}
