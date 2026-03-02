package lazyideavim.whichkeylazy.ideavim

/**
 * A single mapping parsed from .ideavimrc.
 *
 * @param mode "n", "v", "nv", or "" (all modes)
 * @param keySequence The key sequence after leader has been stripped, e.g. "ff" from "<leader>ff"
 * @param actionId The IDE action ID, e.g. "GotoFile"
 * @param isRecursive Whether this is a map (recursive) vs noremap (non-recursive)
 */
data class IdeaVimMapping(
    val mode: String,
    val keySequence: String,
    val actionId: String,
    val isRecursive: Boolean
)

/**
 * Collected results from parsing an ideavimrc file.
 *
 * @param leader The leader key character (default '\')
 * @param mappings All leader-prefixed mappings found
 * @param descriptions Map of key suffix (e.g. "ff") -> description text from g:WhichKeyDesc_ variables
 */
data class IdeaVimParseResult(
    val leader: Char,
    val mappings: List<IdeaVimMapping>,
    val descriptions: Map<String, String>
)
