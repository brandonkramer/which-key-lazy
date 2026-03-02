<img src="src/main/resources/pluginIcon.svg" width="90" height="90" alt="Which Key Lazy logo"/>

# Which Key LazyVim-style

A [which-key](https://github.com/folke/which-key.nvim) LazyVim-style popup for JetBrains IDEs with IdeaVim. Press your leader key and see all available keybindings in a clean, navigable popup — just like LazyVim.

**Requires [IdeaVim](https://plugins.jetbrains.com/plugin/164-ideavim).**

![Which Key Preview](./assets/demo.gif)

## Features

- **Auto-discovers your IdeaVim mappings** — reads leader-prefixed bindings directly from IdeaVim's runtime state (no manual config needed)
- **Hierarchical navigation** — groups like `<leader>b` expand into sub-menus (`bd` delete, `bp` pin, `br` close right, etc.)
- **Icons from your IDE** — action icons come from IntelliJ's action system, so they respect icon theme plugins (Atom Material Icons, etc.)
- **Smart group icons** — groups like +git, +debug, +search get context-aware icons automatically (git branch, debugger, search, etc.)
- **Modifier key support** — displays `<C-n>`, `<S-Tab>`, `<D-\>` and other modifier combos correctly
- **Theme-aware colors** — background, text, and accent colors are pulled from your current editor color scheme
- **LazyVim default group names** — common groups are labeled automatically (`+buffer`, `+code`, `+debug`, `+git`, `+search`, etc.)
- **Customizable descriptions** — override any label using `g:WhichKeyDesc_` variables in your `.ideavimrc`
- **JSON config file** — optional `~/.whichkey-lazy.json` for settings, per-key icon/description overrides, and custom bindings
- **Vertical list layout** — entries flow top-to-bottom, wrapping into columns only when needed
- **Breadcrumb navigation** — see your current path through the key tree

## Quick Start

### 1. Install the plugin

Install **Which Key LazyVim-style** from the JetBrains Marketplace, or build from source.

### 2. Configure your `.ideavimrc`

Add these lines to your `~/.ideavimrc`:

```vim
let mapleader=" "

" Enable the which-key popup
set which-key

" Disable timeout so the popup stays open
set notimeout
```

 `set which-key` activates the plugin through IdeaVim's extension system. When you press your leader key (Space) in normal mode, the popup appears automatically showing all available bindings.

No `nnoremap <Space>` mapping is needed — the plugin observes IdeaVim's key handling and shows the popup as visual feedback while IdeaVim processes your leader key sequence.


### 3. Add leader mappings

The plugin automatically discovers any `nmap <leader>...` mappings:

```vim
" File/Find
nmap <leader>ff :action GotoFile<cr>
nmap <leader>fr :action RecentFiles<cr>
nmap <leader>fs :action SaveAll<cr>

" Buffer
nmap <leader>bd :action CloseContent<cr>
nmap <leader>bp :action PinActiveTabToggle<cr>

" Code
nmap <leader>ca :action RefactoringMenu<cr>
nmap <leader>cf :action Format<cr>
nmap <leader>cr :action RenameElement<cr>

" Git
nmap <leader>gg :action ActivateCommitToolWindow<cr>
nmap <leader>gb :action Annotate<cr>
nmap <leader>gl :action Vcs.Show.Log<cr>
```

Now press `<Space>` in normal mode and the popup appears with all your bindings organized into groups.

## Custom Descriptions

### `g:WhichKeyDesc_` variables

Override any entry's label using `g:WhichKeyDesc_` variables. This follows the same convention as [idea-which-key](https://github.com/TheBlob42/idea-which-key).

**Format:**

```vim
let g:WhichKeyDesc_<unique_name> = "<mapping> <description>"
```

**Examples:**

```vim
" Label a group
let g:WhichKeyDesc_buffer = "<leader>b +buffer"
let g:WhichKeyDesc_code = "<leader>c +code"
let g:WhichKeyDesc_debug = "<leader>d +debug"

" Label individual actions
let g:WhichKeyDesc_find_files = "<leader>ff Find Files"
let g:WhichKeyDesc_recent = "<leader>fr Recent Files"
let g:WhichKeyDesc_save = "<leader>fs Save All"

" Label a nested group
let g:WhichKeyDesc_search_noice = "<leader>sn +noice"
```

**Rules:**
- The variable name after `g:WhichKeyDesc_` can be anything unique (letters, numbers, underscores)
- The value format is: `<key-sequence> <description>`
- `<leader>` in the key sequence is automatically replaced with your actual leader key
- These override both the built-in default names and the auto-detected action names

### Built-in Default Group Names

Common LazyVim group prefixes are labeled automatically when no `g:WhichKeyDesc_` is set:

| Key | Default Label |
|-----|--------------|
| `a` | +ai |
| `b` | +buffer |
| `c` | +code |
| `d` | +debug |
| `f` | +file/find |
| `g` | +git |
| `n` | +notifications |
| `o` | +overseer |
| `q` | +quit/session |
| `r` | +refactor |
| `s` | +search |
| `t` | +test |
| `u` | +ui |
| `w` | +windows |
| `x` | +diagnostics/quickfix |

Groups not in this table fall back to `+<key>` (e.g., `+z`).

### Action Description Priority

For each entry, the description is resolved in this order:

1. `g:WhichKeyDesc_` variable (highest priority)
2. Built-in default group name (for groups)
3. IntelliJ action's display name (from `ActionManager`)
4. Humanized action ID (e.g., `GotoFile` becomes `Go to File`)

## Theme Integration

The popup colors are derived from your current editor color scheme:

- **Background** — editor background color
- **Text** — editor default foreground
- **Key badge / Group labels** — keyword color from the scheme
- **Separator** — blended foreground/background
- **Font** — your editor's configured font and size

This means the popup looks correct with any theme: Darcula, Light, Solarized, Dracula, One Dark, Nord, Tokyo Night, etc. If you change your theme, the popup updates automatically.

## Icons

Action entries display the icon from IntelliJ's action registry. These are the same icons you see in menus and toolbars. If you have an icon theme plugin installed (like [Atom Material Icons](https://plugins.jetbrains.com/plugin/10044-atom-material-icons)), those themed icons will appear in the popup automatically.

Group entries get smart icons based on their description — for example, +git shows a branch icon, +debug shows a debugger icon, +search shows a search icon. Groups that don't match any known keyword fall back to a folder icon. You can override any group's icon via `~/.whichkey-lazy.json` (see below).

Entries without an icon display a small dot placeholder so text stays aligned.

## Configuration

The plugin works out of the box with zero configuration. For advanced customization, create `~/.whichkey-lazy.json`:

```json
{
  "settings": {
    "delay": 200,
    "maxColumns": 5,
    "sortGroupsFirst": true,
    "showIcons": true
  },
  "overrides": {
    "g": { "description": "+git", "icon": "git" },
    "|": { "description": "Split Vertical", "icon": "windows" }
  }
}
```

### Settings

| Setting | Default | Description |
|---------|---------|-------------|
| `delay` | `200` | Milliseconds before popup appears |
| `maxColumns` | `5` | Maximum number of columns in the popup |
| `sortGroupsFirst` | `true` | Show groups before actions |
| `showIcons` | `true` | Show icons next to entries |

### Per-key Overrides

The `overrides` map lets you customize any entry's description or icon by its post-leader key sequence. The `icon` value can be:
- A keyword shorthand: `"git"`, `"debug"`, `"search"`, `"windows"`, `"code"`, `"test"`, etc.
- A full AllIcons path: `"AllIcons.Actions.Undo"`, `"AllIcons.Vcs.Branch"`, etc.

## Full LazyVim-style `.ideavimrc` Example

Here's a comprehensive example that mirrors LazyVim's default keybindings:

```vim
let mapleader=" "
set which-key
set notimeout

" ── Group descriptions ──────────────────────────────
let g:WhichKeyDesc_buffer     = "<leader>b +buffer"
let g:WhichKeyDesc_code       = "<leader>c +code"
let g:WhichKeyDesc_debug      = "<leader>d +debug"
let g:WhichKeyDesc_file       = "<leader>f +file/find"
let g:WhichKeyDesc_git        = "<leader>g +git"
let g:WhichKeyDesc_quit       = "<leader>q +quit/session"
let g:WhichKeyDesc_search     = "<leader>s +search"
let g:WhichKeyDesc_test       = "<leader>t +test"
let g:WhichKeyDesc_ui         = "<leader>u +ui"
let g:WhichKeyDesc_windows    = "<leader>w +windows"
let g:WhichKeyDesc_diagnostics = "<leader>x +diagnostics"

" ── File/Find ───────────────────────────────────────
nmap <leader><space> :action GotoFile<cr>
nmap <leader>ff :action GotoFile<cr>
nmap <leader>fr :action RecentFiles<cr>
nmap <leader>fn :action NewElementSamePlace<cr>
nmap <leader>fb :action Switcher<cr>
nmap <leader>ft :action ActivateTerminalToolWindow<cr>
nmap <leader>fe :action ActivateProjectToolWindow<cr>
nmap <leader>fs :action SaveAll<cr>

" ── Buffer ──────────────────────────────────────────
nmap <leader>bd :action CloseContent<cr>
nmap <leader>bo :action CloseAllEditorsButActive<cr>
nmap <leader>bp :action PinActiveTabToggle<cr>
nmap <leader>bl :action CloseAllToTheLeft<cr>
nmap <leader>br :action CloseAllToTheRight<cr>

" ── Code ────────────────────────────────────────────
nmap <leader>ca :action RefactoringMenu<cr>
nmap <leader>cf :action Format<cr>
nmap <leader>cr :action RenameElement<cr>
nmap <leader>cR :action RenameFile<cr>
nmap <leader>cd :action ActivateProblemsViewToolWindow<cr>

" ── Debug ───────────────────────────────────────────
nmap <leader>db :action ToggleLineBreakpoint<cr>
nmap <leader>dc :action Resume<cr>
nmap <leader>di :action StepInto<cr>
nmap <leader>do :action StepOut<cr>
nmap <leader>dO :action StepOver<cr>
nmap <leader>dt :action Stop<cr>

" ── Git ─────────────────────────────────────────────
nmap <leader>gg :action ActivateCommitToolWindow<cr>
nmap <leader>gb :action Annotate<cr>
nmap <leader>gl :action Vcs.Show.Log<cr>
nmap <leader>gs :action Vcs.Show.Log<cr>

" ── Search ──────────────────────────────────────────
nmap <leader>sg :action FindInPath<cr>
nmap <leader>ss :action GotoSymbol<cr>
nmap <leader>sh :action HelpTopics<cr>
nmap <leader>sk :map<cr>
nmap <leader>sm :marks<cr>
nmap <leader>so :action ShowSettings<cr>

" ── Quit ────────────────────────────────────────────
nmap <leader>qq :action Exit<cr>

" ── UI ──────────────────────────────────────────────
nmap <leader>uw :setlocal wrap!<cr>
nmap <leader>ul :set number!<cr>
nmap <leader>uL :set relativenumber!<cr>
nmap <leader>uc :action QuickChangeScheme<cr>

" ── Windows ─────────────────────────────────────────
nmap <leader>wd :action CloseContent<cr>
nmap <leader>wm :action ToggleDistractionFreeMode<cr>
nmap <leader>- <c-w>s
nmap <leader>| <c-w>v
```

## Development

```bash
# Build
./gradlew buildPlugin                  # Build distribution zip → build/distributions/

# Run
./gradlew runIde                       # Launch sandbox IDE with plugin loaded

# Test
./gradlew test                         # Run all tests
./gradlew test --tests "*.IconResolverTest"  # Run a specific test class

# Utilities
./gradlew compileKotlin                # Fast compile check (no tests)
./gradlew verifyPlugin                 # Verify plugin structure
./gradlew tasks                        # List all available tasks
```


## License

MIT
