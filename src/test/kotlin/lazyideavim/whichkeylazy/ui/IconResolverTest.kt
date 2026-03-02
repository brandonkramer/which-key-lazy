package lazyideavim.whichkeylazy.ui

import com.intellij.icons.AllIcons
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Tests for IconResolver — keyword matching, group icon resolution,
 * and AllIcons path reflection.
 *
 * Uses BasePlatformTestCase because AllIcons requires the IntelliJ platform.
 */
class IconResolverTest : BasePlatformTestCase() {

    // --- resolveGroupIcon: keyword matching from description ---

    fun testGitDescriptionResolvesToBranchIcon() {
        val icon = IconResolver.resolveGroupIcon("+git")
        assertEquals(AllIcons.Vcs.Branch, icon)
    }

    fun testDebugDescriptionResolvesToDebuggerIcon() {
        val icon = IconResolver.resolveGroupIcon("+debug")
        assertEquals(AllIcons.Actions.StartDebugger, icon)
    }

    fun testCodeDescriptionResolvesToIntentionIcon() {
        val icon = IconResolver.resolveGroupIcon("+code")
        assertEquals(AllIcons.Actions.IntentionBulb, icon)
    }

    fun testFileDescriptionResolvesToFindIcon() {
        val icon = IconResolver.resolveGroupIcon("+file/find")
        assertEquals(AllIcons.Actions.Find, icon)
    }

    fun testSearchDescriptionResolvesToSearchIcon() {
        val icon = IconResolver.resolveGroupIcon("+search")
        assertEquals(AllIcons.Actions.Search, icon)
    }

    fun testWindowsDescriptionResolvesToSplitIcon() {
        val icon = IconResolver.resolveGroupIcon("+windows")
        assertEquals(AllIcons.Actions.SplitVertically, icon)
    }

    fun testUnknownDescriptionFallsBackToFolderIcon() {
        val icon = IconResolver.resolveGroupIcon("+unknown-group")
        assertEquals(IconResolver.GROUP_ICON, icon)
    }

    fun testEmptyDescriptionFallsBackToFolderIcon() {
        val icon = IconResolver.resolveGroupIcon("")
        assertEquals(IconResolver.GROUP_ICON, icon)
    }

    fun testCaseInsensitiveMatching() {
        val icon = IconResolver.resolveGroupIcon("+GIT")
        assertEquals(AllIcons.Vcs.Branch, icon)
    }

    fun testPartialMatchInDescription() {
        // "+file/find" should match "file" keyword
        val icon = IconResolver.resolveGroupIcon("+file/find")
        assertEquals(AllIcons.Actions.Find, icon)
    }

    // --- resolveGroupIcon: configuredIcon override ---

    fun testConfiguredIconOverridesDescription() {
        // Description says "search" but config says "git"
        val icon = IconResolver.resolveGroupIcon("+search", configuredIcon = "git")
        assertEquals(AllIcons.Vcs.Branch, icon)
    }

    fun testConfiguredIconKeywordIsCaseInsensitive() {
        val icon = IconResolver.resolveGroupIcon("+something", configuredIcon = "GIT")
        assertEquals(AllIcons.Vcs.Branch, icon)
    }

    fun testBlankConfiguredIconFallsBackToDescription() {
        val icon = IconResolver.resolveGroupIcon("+git", configuredIcon = "")
        assertEquals(AllIcons.Vcs.Branch, icon)
    }

    fun testNullConfiguredIconFallsBackToDescription() {
        val icon = IconResolver.resolveGroupIcon("+debug", configuredIcon = null)
        assertEquals(AllIcons.Actions.StartDebugger, icon)
    }

    // --- resolveIconByKeyword ---

    fun testKeywordShorthandGit() {
        val icon = IconResolver.resolveIconByKeyword("git")
        assertEquals(AllIcons.Vcs.Branch, icon)
    }

    fun testKeywordShorthandWindows() {
        val icon = IconResolver.resolveIconByKeyword("windows")
        assertEquals(AllIcons.Actions.SplitVertically, icon)
    }

    fun testKeywordShorthandIsCaseInsensitive() {
        val icon = IconResolver.resolveIconByKeyword("DEBUG")
        assertEquals(AllIcons.Actions.StartDebugger, icon)
    }

    fun testInvalidKeywordReturnsNull() {
        val icon = IconResolver.resolveIconByKeyword("nonexistent")
        assertNull(icon)
    }

    fun testNullKeywordReturnsNull() {
        val icon = IconResolver.resolveIconByKeyword(null)
        assertNull(icon)
    }

    fun testBlankKeywordReturnsNull() {
        val icon = IconResolver.resolveIconByKeyword("  ")
        assertNull(icon)
    }

    // --- resolveIconByKeyword: AllIcons path resolution ---

    fun testAllIconsPathResolution() {
        val icon = IconResolver.resolveIconByKeyword("AllIcons.Actions.Find")
        assertEquals(AllIcons.Actions.Find, icon)
    }

    fun testAllIconsPathResolutionForVcs() {
        val icon = IconResolver.resolveIconByKeyword("AllIcons.Vcs.Branch")
        assertEquals(AllIcons.Vcs.Branch, icon)
    }

    fun testInvalidAllIconsPathReturnsNull() {
        val icon = IconResolver.resolveIconByKeyword("AllIcons.Nonexistent.Icon")
        assertNull(icon)
    }

    fun testPartialAllIconsPathReturnsNull() {
        val icon = IconResolver.resolveIconByKeyword("AllIcons.Actions")
        assertNull(icon)
    }

    fun testNonAllIconsPrefixPathReturnsNull() {
        val icon = IconResolver.resolveIconByKeyword("SomeIcons.Actions.Find")
        assertNull(icon)
    }

    // --- GROUP_ICON ---

    fun testGroupIconIsFolderIcon() {
        assertEquals(AllIcons.Nodes.Folder, IconResolver.GROUP_ICON)
    }
}
