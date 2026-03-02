# CLAUDE.md

Guidance for Claude Code when working with which-key-lazy.

## Commands

```bash
# Development
./gradlew compileKotlin                # Fast compile check
./gradlew runIde                       # Launch sandbox IDE with plugin loaded
./gradlew buildPlugin                  # Build distribution zip (build/distributions/)

# Testing
./gradlew test                         # Run all tests
./gradlew test --tests "*.DefaultConfigTest"           # Run one test class
./gradlew test --tests "*.IdeaVimRcParserTest.parses space leader"  # Run one test method
./gradlew compileTestKotlin            # Compile tests without running

# Verification
./gradlew verifyPlugin                 # Verify plugin structure
./gradlew verifyPluginProjectConfiguration  # Check build config

# Utilities
./gradlew tasks                        # List all available tasks
./gradlew dependencies                 # Show dependency tree
```

Add `--console=plain` for cleaner output. Add `JAVA_HOME` export if Gradle can't find JDK.

## Project Overview

**Which Key LazyVim-style** is a JetBrains IDE plugin that replicates the `which-key.nvim` popup from Neovim/LazyVim. It integrates with IdeaVim to show leader-key bindings hierarchically when the user presses a prefix key and pauses.

Activated via `set which-key` in `.ideavimrc`. No separate keybinding needed.

## Architecture

### Data Flow

```
Keystroke → WhichKeyActionListener (AnActionListener)
         → MappingLookup.getNestedEntries(keySequence, mode)
           ├── tryLeaderLookup (pre-built tree from IdeaVimApiReader)
           └── queryAllMappings (runtime query: user mappings + VIM_ACTIONS)
         → WhichKeyPopupManager.showPopup(entries)
         → WhichKeyPopup → WhichKeyPanel (renders)
```

### Package Layout

```
lazyideavim.whichkeylazy/
├── WhichKeyVimExtension.kt      # IdeaVim extension entry point (set which-key)
├── WhichKeyAction.kt            # Manual trigger action (Tools menu)
├── WhichKeyStartup.kt           # ProjectActivity for pre-loading config
├── WhichKeyPluginDisposable.kt  # App-scoped disposable
│
├── config/                      # Configuration & file management
│   ├── WhichKeyConfigService.kt # App service — rootBindings, settings, overrides
│   ├── WhichKeyConfig.kt        # JSON config loader (~/.whichkey-lazy.json)
│   ├── DefaultConfig.kt         # Default config template
│   ├── ConfigFileWatcher.kt     # Watches config file for changes
│   └── ReloadConfigAction.kt    # Reloads config
│
├── dispatch/                    # Key event handling & popup lifecycle
│   ├── WhichKeyActionListener.kt # Core keystroke observer
│   ├── MappingLookup.kt         # Key sequence → nested entries resolution
│   ├── WhichKeyPopupManager.kt  # Stateless popup lifecycle
│   └── ActionExecutor.kt        # Executes IntelliJ actions
│
├── ideavim/                     # IdeaVim integration & parsing
│   ├── IdeaVimApiReader.kt      # Preferred: reads mappings via IdeaVim runtime API
│   ├── IdeaVimRcParser.kt       # Fallback: parses ~/.ideavimrc file
│   ├── IdeaVimTreeBuilder.kt    # Converts flat mappings → KeyNode tree
│   ├── IdeaVimMapping.kt        # Data classes for parsed mappings
│   ├── DefaultGroupNames.kt     # LazyVim group name fallbacks (+git, +code, etc.)
│   └── KeyNodeMerger.kt         # Merges binding trees
│
├── model/                       # Data model
│   ├── KeyNode.kt               # Sealed class: GroupNode | ActionNode (String keys)
│   └── KeyBinding.kt            # Serialization models for JSON config
│
└── ui/                          # User interface & rendering
    ├── WhichKeyPanel.kt         # Custom JPanel — columnar layout rendering
    ├── WhichKeyPopup.kt         # JBPopup wrapper with keyboard navigation
    ├── BreadcrumbBar.kt         # Shows current path in key tree
    ├── IconResolver.kt          # Maps group descriptions → AllIcons
    └── WhichKeyColors.kt        # Theme-aware colors from editor scheme
```

### Key Design Decisions

- **String-based keys**: `KeyNode.key` is `String` (IdeaVim notation like `"f"`, `"<Tab>"`, `"<C-n>"`), not `Char`. This allows lossless representation of all key types including modifier combos.
- **Dual data source**: Prefers IdeaVim runtime API (`IdeaVimApiReader`), falls back to `.ideavimrc` file parsing (`IdeaVimRcParser` + `IdeaVimTreeBuilder`).
- **Runtime query + enrichment**: `MappingLookup` merges two sources — `queryAllMappings` (runtime, mode-aware, determines structure) and `tryLeaderLookup` (pre-built tree, provides richer icons/descriptions).
- **Stateless popup**: Popup is destroyed and recreated on every keystroke, matching idea-which-key's approach. No navigation stack.
- **Theme-aware**: All colors come from `EditorColorsManager.schemeForCurrentUITheme`.
- **Mode filtering**: Pre-built leader tree only used for NORMAL/VISUAL modes. INSERT mode relies solely on runtime query to avoid showing normal-mode mappings.

## IdeaVim Integration Notes

- Uses `injector.parser.toKeyNotation(keyStroke)` for lossless keystroke → string conversion
- Uses `injector.keyGroup.getKeyMapping(mode)` and `getAll(prefix)` for efficient prefix queries
- `<Plug>` and `<Action>` pseudo-keys (internal IdeaVim constructs) are filtered from the LHS of mappings
- Reads `g:WhichKeyDesc_` variables from `injector.variableService` for custom descriptions
- Reads `mapleader` from `injector.variableService` for leader key resolution
- Checks `which-key` option via `injector.optionGroup` to respect `set which-key` / `set nowhich-key`
- Suppresses popup when `commandBuilder.expectedArgumentType == Argument.Type.DIGRAPH` (after f/t/r etc.)

## Config Files

- **IdeaVim mappings**: `~/.ideavimrc` (or XDG `~/.config/ideavim/ideavimrc`)
- **Plugin overrides**: `~/.whichkey-lazy.json` — settings, per-key icon/description overrides
- **Plugin manifest**: `src/main/resources/META-INF/plugin.xml`

## Active Technologies

- Kotlin (JVM 21) + IntelliJ Platform SDK (2025.1.1)
- IdeaVim 2.27.2 (plugin dependency)
- kotlinx-serialization-json 1.7.3
- IntelliJ Platform Gradle Plugin

## Reference Project

This plugin is inspired by [idea-which-key](https://github.com/TheBlob42/idea-which-key)
