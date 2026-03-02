package lazyideavim.whichkeylazy.ideavim

import lazyideavim.whichkeylazy.model.KeyNode
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Tests for IdeaVimTreeBuilder — converting flat IdeaVim mappings
 * into a hierarchical KeyNode tree.
 *
 * Uses BasePlatformTestCase because buildTree() calls IconResolver
 * which requires AllIcons (IntelliJ platform icons).
 */
class IdeaVimTreeBuilderTest : BasePlatformTestCase() {

    fun testSingleActionMapping() {
        val result = IdeaVimParseResult(
            leader = ' ',
            mappings = listOf(
                IdeaVimMapping("n", "q", "Exit", isRecursive = false)
            ),
            descriptions = emptyMap()
        )
        val tree = IdeaVimTreeBuilder.buildTree(result)

        assertEquals(1, tree.size)
        val node = tree["q"]
        assertTrue("Should be ActionNode", node is KeyNode.ActionNode)
        assertEquals("Exit", (node as KeyNode.ActionNode).actionId)
    }

    fun testMultipleMappingsCreateGroup() {
        val result = IdeaVimParseResult(
            leader = ' ',
            mappings = listOf(
                IdeaVimMapping("n", "ff", "GotoFile", isRecursive = false),
                IdeaVimMapping("n", "fr", "RecentFiles", isRecursive = false)
            ),
            descriptions = emptyMap()
        )
        val tree = IdeaVimTreeBuilder.buildTree(result)

        assertEquals("Should have 1 top-level group 'f'", 1, tree.size)
        val fGroup = tree["f"]
        assertTrue("'f' should be GroupNode", fGroup is KeyNode.GroupNode)

        val children = (fGroup as KeyNode.GroupNode).children
        assertEquals(2, children.size)
        assertTrue("Should have 'f' child", children.containsKey("f"))
        assertTrue("Should have 'r' child", children.containsKey("r"))
        assertEquals("GotoFile", (children["f"] as KeyNode.ActionNode).actionId)
        assertEquals("RecentFiles", (children["r"] as KeyNode.ActionNode).actionId)
    }

    fun testWhichKeyDescOverridesDescription() {
        val result = IdeaVimParseResult(
            leader = ' ',
            mappings = listOf(
                IdeaVimMapping("n", "ff", "GotoFile", isRecursive = false)
            ),
            descriptions = mapOf(
                "ff" to "Custom Find File Description"
            )
        )
        val tree = IdeaVimTreeBuilder.buildTree(result)

        val fGroup = tree["f"] as KeyNode.GroupNode
        val ffNode = fGroup.children["f"] as KeyNode.ActionNode
        assertEquals("Custom Find File Description", ffNode.description)
    }

    fun testGroupDescriptionFromWhichKeyDesc() {
        val result = IdeaVimParseResult(
            leader = ' ',
            mappings = listOf(
                IdeaVimMapping("n", "ff", "GotoFile", isRecursive = false),
                IdeaVimMapping("n", "fr", "RecentFiles", isRecursive = false)
            ),
            descriptions = mapOf(
                "f" to "+file/find"
            )
        )
        val tree = IdeaVimTreeBuilder.buildTree(result)

        val fGroup = tree["f"] as KeyNode.GroupNode
        assertEquals("+file/find", fGroup.description)
    }

    fun testGroupDescriptionFallsBackToDefaultGroupNames() {
        val result = IdeaVimParseResult(
            leader = ' ',
            mappings = listOf(
                IdeaVimMapping("n", "gf", "Vcs.Show.Log", isRecursive = false),
                IdeaVimMapping("n", "gs", "Vcs.Show.Local.Changes", isRecursive = false)
            ),
            descriptions = emptyMap()
        )
        val tree = IdeaVimTreeBuilder.buildTree(result)

        val gGroup = tree["g"] as KeyNode.GroupNode
        // DefaultGroupNames maps "g" -> "+git"
        assertEquals("+git", gGroup.description)
    }

    fun testFiltersToNormalModeMappingsOnly() {
        val result = IdeaVimParseResult(
            leader = ' ',
            mappings = listOf(
                IdeaVimMapping("n", "ff", "GotoFile", isRecursive = false),
                IdeaVimMapping("v", "ff", "GotoFile", isRecursive = false),
                IdeaVimMapping("nv", "gg", "ActivateCommitToolWindow", isRecursive = false)
            ),
            descriptions = emptyMap()
        )
        val tree = IdeaVimTreeBuilder.buildTree(result)

        // "n" and "nv" modes should be included, "v" only should be excluded
        // ff appears in both n and v, but tree should have it from n
        // gg appears in nv, should be included
        val fGroup = tree["f"] as KeyNode.GroupNode
        assertNotNull(fGroup.children["f"])
        val gGroup = tree["g"] as KeyNode.GroupNode
        assertNotNull(gGroup.children["g"])
    }

    fun testEmptyMappingsReturnEmptyTree() {
        val result = IdeaVimParseResult(
            leader = ' ',
            mappings = emptyList(),
            descriptions = emptyMap()
        )
        val tree = IdeaVimTreeBuilder.buildTree(result)
        assertTrue(tree.isEmpty())
    }

    fun testThreeLevelDeepMapping() {
        val result = IdeaVimParseResult(
            leader = ' ',
            mappings = listOf(
                IdeaVimMapping("n", "snd", "ClearAllNotifications", isRecursive = false)
            ),
            descriptions = emptyMap()
        )
        val tree = IdeaVimTreeBuilder.buildTree(result)

        val sGroup = tree["s"] as KeyNode.GroupNode
        val snGroup = sGroup.children["n"] as KeyNode.GroupNode
        val sndAction = snGroup.children["d"] as KeyNode.ActionNode
        assertEquals("ClearAllNotifications", sndAction.actionId)
    }

    fun testMixedGroupAndActionAtSameLevel() {
        val result = IdeaVimParseResult(
            leader = ' ',
            mappings = listOf(
                IdeaVimMapping("n", "ff", "GotoFile", isRecursive = false),
                IdeaVimMapping("n", "q", "Exit", isRecursive = false)
            ),
            descriptions = emptyMap()
        )
        val tree = IdeaVimTreeBuilder.buildTree(result)

        assertEquals(2, tree.size)
        assertTrue("'f' should be a group", tree["f"] is KeyNode.GroupNode)
        assertTrue("'q' should be an action", tree["q"] is KeyNode.ActionNode)
    }

    fun testSpecialCharInKeySequence() {
        // Tab char (0x09) is used by IdeaVimRcParser for <Tab>
        val result = IdeaVimParseResult(
            leader = ' ',
            mappings = listOf(
                IdeaVimMapping("n", "\tf", "GoToTab1", isRecursive = false),
                IdeaVimMapping("n", "\tl", "GoToLastTab", isRecursive = false)
            ),
            descriptions = emptyMap()
        )
        val tree = IdeaVimTreeBuilder.buildTree(result)

        // The tab char should be converted to "<Tab>" notation
        assertTrue("Should have '<Tab>' group", tree.containsKey("<Tab>"))
        val tabGroup = tree["<Tab>"] as KeyNode.GroupNode
        assertEquals(2, tabGroup.children.size)
    }
}
