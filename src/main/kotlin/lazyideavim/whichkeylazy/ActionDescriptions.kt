package lazyideavim.whichkeylazy

import com.intellij.openapi.actionSystem.ActionManager

/**
 * Shared utilities for resolving human-readable descriptions from IntelliJ action IDs.
 */
object ActionDescriptions {

    fun lookup(actionId: String): String {
        if (actionId.isBlank()) return "???"
        try {
            val action = ActionManager.getInstance().getAction(actionId)
            val text = action?.templatePresentation?.text
            if (!text.isNullOrBlank()) return text
        } catch (_: Exception) { }
        return humanize(actionId)
    }

    fun humanize(actionId: String): String {
        return actionId
            .replace(".", " ")
            .replace(Regex("([a-z])([A-Z])")) { "${it.groupValues[1]} ${it.groupValues[2]}" }
            .replace(Regex("([A-Z]+)([A-Z][a-z])")) { "${it.groupValues[1]} ${it.groupValues[2]}" }
            .trim()
    }
}
