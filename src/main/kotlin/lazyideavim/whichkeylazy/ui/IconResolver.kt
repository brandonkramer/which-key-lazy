package lazyideavim.whichkeylazy.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import javax.swing.Icon

object IconResolver {

    val GROUP_ICON: Icon = AllIcons.Nodes.Folder

    private val DEFAULT_GROUP_ICONS: Map<String, Icon> = mapOf(
        "git"           to AllIcons.Vcs.Branch,
        "debug"         to AllIcons.Actions.StartDebugger,
        "code"          to AllIcons.Actions.IntentionBulb,
        "file"          to AllIcons.Actions.Find,
        "find"          to AllIcons.Actions.Find,
        "buffer"        to AllIcons.FileTypes.Text,
        "search"        to AllIcons.Actions.Search,
        "test"          to AllIcons.RunConfigurations.TestState.Run,
        "ui"            to AllIcons.General.Settings,
        "windows"       to AllIcons.Actions.SplitVertically,
        "diagnostics"   to AllIcons.General.InspectionsEye,
        "quickfix"      to AllIcons.General.InspectionsEye,
        "quit"          to AllIcons.Actions.Exit,
        "session"       to AllIcons.Actions.Exit,
        "refactor"      to AllIcons.Actions.RefactoringBulb,
        "notifications" to AllIcons.General.BalloonInformation,
        "ai"            to AllIcons.Actions.Lightning,
        "overseer"      to AllIcons.Actions.Execute,
        "noice"         to AllIcons.General.BalloonInformation,
        "tab"           to AllIcons.Actions.OpenNewTab,
    )

    /**
     * Priority: configuredIcon > keyword match on description > GROUP_ICON fallback.
     */
    fun resolveGroupIcon(description: String, configuredIcon: String? = null): Icon {
        if (!configuredIcon.isNullOrBlank()) {
            resolveIconByKeyword(configuredIcon)?.let { return it }
        }
        val descLower = description.lowercase()
        for ((keyword, icon) in DEFAULT_GROUP_ICONS) {
            if (descLower.contains(keyword)) return icon
        }
        return GROUP_ICON
    }

    /**
     * Resolve an icon by keyword shorthand (e.g. "git") or full AllIcons path
     * (e.g. "AllIcons.Actions.Undo"). Returns null if not recognized.
     */
    fun resolveIconByKeyword(keyword: String?): Icon? {
        if (keyword.isNullOrBlank()) return null

        // 1. Try keyword shorthand
        DEFAULT_GROUP_ICONS[keyword.lowercase()]?.let { return it }

        // 2. Try full AllIcons path (e.g., "AllIcons.Actions.Undo")
        if (keyword.startsWith("AllIcons.")) {
            return resolveAllIconsPath(keyword)
        }

        return null
    }

    private val allIconsCache = mutableMapOf<String, Icon?>()

    private fun resolveAllIconsPath(path: String): Icon? {
        return allIconsCache.getOrPut(path) {
            try {
                // "AllIcons.Actions.Undo" → ["AllIcons", "Actions", "Undo"]
                val parts = path.split(".")
                if (parts.size < 3 || parts[0] != "AllIcons") return@getOrPut null

                // Walk nested classes: AllIcons$Actions$Undo or AllIcons.Actions field Undo
                var clazz: Class<*> = AllIcons::class.java
                for (i in 1 until parts.size - 1) {
                    clazz = clazz.declaredClasses.firstOrNull { it.simpleName == parts[i] }
                        ?: return@getOrPut null
                }

                val field = clazz.getDeclaredField(parts.last())
                field.isAccessible = true
                field.get(null) as? Icon
            } catch (_: Exception) {
                null
            }
        }
    }

    fun resolveActionIcon(actionId: String?): Icon? {
        if (actionId.isNullOrBlank()) return null
        return try {
            ActionManager.getInstance().getAction(actionId)?.templatePresentation?.icon
        } catch (_: Exception) {
            null
        }
    }
}
