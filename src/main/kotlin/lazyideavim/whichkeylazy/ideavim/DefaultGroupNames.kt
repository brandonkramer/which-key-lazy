package lazyideavim.whichkeylazy.ideavim

/**
 * Well-known LazyVim group names for common leader key prefixes.
 * Used as fallback when no g:WhichKeyDesc_ variable is set.
 */
object DefaultGroupNames {

    private val DEFAULTS = mapOf(
        "a" to "+ai",
        "b" to "+buffer",
        "c" to "+code",
        "d" to "+debug",
        "f" to "+file/find",
        "g" to "+git",
        "n" to "+notifications",
        "o" to "+overseer",
        "q" to "+quit/session",
        "r" to "+refactor",
        "s" to "+search",
        "t" to "+test",
        "u" to "+ui",
        "w" to "+windows",
        "x" to "+diagnostics/quickfix",
        "sn" to "+noice",
    )

    /**
     * Returns the default group description for a key path, or null.
     * [keyPath] is the key sequence after leader (e.g. "b", "sn", "f").
     */
    fun getDefault(keyPath: String): String? = DEFAULTS[keyPath]
}
