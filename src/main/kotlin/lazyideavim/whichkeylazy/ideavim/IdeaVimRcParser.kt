package lazyideavim.whichkeylazy.ideavim

import java.io.File

/**
 * Parses ~/.ideavimrc to extract leader-prefixed IDE action mappings,
 * g:WhichKeyDesc_ descriptions, and the mapleader value.
 *
 * Uses a two-pass approach:
 *   Pass 1: Collect all lines (following `source` directives), find `let mapleader`
 *   Pass 2: Parse mappings and descriptions using the resolved leader
 *
 * This handles the common case where `let mapleader = " "` appears after
 * the leader-prefixed mappings in the file.
 */
object IdeaVimRcParser {

    private val LEADER_REGEX = Regex(
        """^\s*let\s+mapleader\s*=\s*["'](.+?)["']\s*$"""
    )

    private val MAP_REGEX = Regex(
        """^\s*(n|v|x)?(nore)?map\s+(.+?)\s+(.+)$"""
    )

    private val ACTION_COLON_REGEX = Regex(
        """:action\s+(\S+)"""
    )

    private val ACTION_PAREN_REGEX = Regex(
        """<[Aa]ction>\((\S+)\)"""
    )

    private val WHICHKEY_DESC_REGEX = Regex(
        """^\s*let\s+g:WhichKeyDesc_\S+\s*=\s*["'](.+?)["']\s*$"""
    )

    private val SOURCE_REGEX = Regex(
        """^\s*source\s+(.+)$"""
    )

    private val MODIFIER_KEY_REGEX = Regex("""<[CcSsAaMmDd]-[^>]*>""")

    val IDEAVIMRC_FILE: File
        get() {
            val xdgConfig = System.getenv("XDG_CONFIG_HOME")
            if (xdgConfig != null) {
                val xdgFile = File(xdgConfig, "ideavim/ideavimrc")
                if (xdgFile.exists()) return xdgFile
            }
            val xdgDefault = File(System.getProperty("user.home"), ".config/ideavim/ideavimrc")
            if (xdgDefault.exists()) return xdgDefault
            return File(System.getProperty("user.home"), ".ideavimrc")
        }

    fun parse(file: File = IDEAVIMRC_FILE): IdeaVimParseResult {
        if (!file.exists()) {
            return IdeaVimParseResult(leader = '\\', mappings = emptyList(), descriptions = emptyMap())
        }

        // Pass 1: Collect all lines (following source directives) and find mapleader
        val allLines = mutableListOf<String>()
        collectLines(file, mutableSetOf(), allLines)

        var leader = '\\'
        for (line in allLines) {
            try {
                LEADER_REGEX.find(line.trim())?.let { match ->
                    val leaderValue = match.groupValues[1]
                    leader = resolveSpecialKey(leaderValue)?.firstOrNull() ?: leaderValue.first()
                }
            } catch (_: Exception) { }
        }

        // Pass 2: Parse mappings and descriptions using the resolved leader
        val mappings = mutableListOf<IdeaVimMapping>()
        val descriptions = mutableMapOf<String, String>()

        for (line in allLines) {
            try {
                parseLine(line.trim(), leader, mappings, descriptions)
            } catch (_: Exception) { }
        }

        return IdeaVimParseResult(leader = leader, mappings = mappings, descriptions = descriptions)
    }

    /**
     * Recursively collects all lines from a file and its `source`d files.
     * Cycle detection via visited set of canonical paths.
     */
    private fun collectLines(
        file: File,
        visited: MutableSet<String>,
        allLines: MutableList<String>
    ) {
        val canonical = try { file.canonicalPath } catch (_: Exception) { return }
        if (!visited.add(canonical)) return
        if (!file.exists() || !file.isFile) return

        for (rawLine in file.readLines()) {
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("\"")) continue

            val sourceMatch = SOURCE_REGEX.find(trimmed)
            if (sourceMatch != null) {
                val sourcePath = resolveSourcePath(sourceMatch.groupValues[1].trim(), file.parentFile)
                collectLines(sourcePath, visited, allLines)
            } else {
                allLines.add(rawLine)
            }
        }
    }

    private fun parseLine(
        line: String,
        leaderChar: Char,
        mappings: MutableList<IdeaVimMapping>,
        descriptions: MutableMap<String, String>
    ) {
        if (line.isEmpty() || line.startsWith("\"")) return

        WHICHKEY_DESC_REGEX.find(line)?.let { match ->
            parseDescription(match.groupValues[1], leaderChar, descriptions)
            return
        }

        MAP_REGEX.find(line)?.let { match ->
            parseMapping(match, leaderChar, mappings)
        }
    }

    private fun parseMapping(
        match: MatchResult,
        leaderChar: Char,
        mappings: MutableList<IdeaVimMapping>
    ) {
        val modePrefix = match.groupValues[1]
        val norePrefix = match.groupValues[2]
        val lhs = match.groupValues[3]
        val rhs = match.groupValues[4]

        val mode = when (modePrefix) {
            "n" -> "n"
            "v", "x" -> "v"
            "" -> "nv"
            else -> return
        }
        val isRecursive = norePrefix.isEmpty()

        val actionId = ACTION_COLON_REGEX.find(rhs)?.groupValues?.get(1)
            ?: ACTION_PAREN_REGEX.find(rhs)?.groupValues?.get(1)
            ?: return
        val cleanActionId = actionId.replace(Regex("<[Cc][Rr]>$"), "")

        val keySequence = extractLeaderSequence(lhs, leaderChar) ?: return

        if (MODIFIER_KEY_REGEX.containsMatchIn(keySequence)) return

        val resolvedSequence = keySequence
            .replace("<Tab>", "\t", ignoreCase = true)
            .replace("<BS>", "\u0008", ignoreCase = true)
            .replace("<Backspace>", "\u0008", ignoreCase = true)
            .replace("<Del>", "\u007f", ignoreCase = true)
            .replace("<Delete>", "\u007f", ignoreCase = true)

        val cleanSequence = resolvedSequence.replace(Regex("<[^>]+>"), "")
        if (cleanSequence.isEmpty()) return

        mappings.add(
            IdeaVimMapping(
                mode = mode,
                keySequence = cleanSequence,
                actionId = cleanActionId,
                isRecursive = isRecursive
            )
        )
    }

    private fun parseDescription(
        value: String,
        leaderChar: Char,
        descriptions: MutableMap<String, String>
    ) {
        val normalized = value
            .replace("<leader>", leaderChar.toString(), ignoreCase = true)
            .replace("<Space>", " ", ignoreCase = true)

        val leaderIdx = normalized.indexOf(leaderChar)
        if (leaderIdx < 0) return

        val afterLeader = normalized.substring(leaderIdx + 1)
        val spaceIdx = afterLeader.indexOf(' ')
        if (spaceIdx <= 0) return

        val keySuffix = afterLeader.substring(0, spaceIdx)
        val desc = afterLeader.substring(spaceIdx + 1).trim()
        if (keySuffix.isNotEmpty() && desc.isNotEmpty()) {
            descriptions[keySuffix] = desc
        }
    }

    private fun extractLeaderSequence(lhs: String, leaderChar: Char): String? {
        val normalized = lhs.trim()
            .replace("<leader>", leaderChar.toString(), ignoreCase = true)
            .replace("<Space>", " ", ignoreCase = true)

        if (normalized.isEmpty() || normalized[0] != leaderChar) return null
        return normalized.substring(1)
    }

    private fun resolveSpecialKey(value: String): String? {
        return when (value.lowercase()) {
            "<space>", " " -> " "
            "<bs>" -> "\b"
            "<tab>" -> "\t"
            "\\\\" -> "\\"
            else -> value
        }
    }

    private fun resolveSourcePath(path: String, parentDir: File?): File {
        val home = System.getProperty("user.home")
        val xdg = System.getenv("XDG_CONFIG_HOME") ?: "$home/.config"
        val expanded = path
            .replace("~", home)
            .replace("\$HOME", home)
            .replace("\$XDG_CONFIG_HOME", xdg)

        val file = File(expanded)
        return if (file.isAbsolute) file else File(parentDir, expanded)
    }
}
