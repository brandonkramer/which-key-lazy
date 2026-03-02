package lazyideavim.whichkeylazy.config

object DefaultConfig {

    val JSON_TEMPLATE = """
{
  "settings": {
    "delay": 200,
    "maxColumns": 5,
    "sortGroupsFirst": true,
    "position": "bottom-right",
    "showIcons": true
  },
  "overrides": {
    "|": { "icon": "AllIcons.Actions.SplitVertically", "description": "Split Right" },
    "-": { "icon": "AllIcons.Actions.SplitHorizontally", "description": "Split Below" },
    "l": { "icon": "AllIcons.Nodes.Plugin"  },
    "o": { "icon": "AllIcons.General.Close"  },
    "K": { "icon": "AllIcons.Actions.Help", "description":  "Help" },
    "`": { "icon": "AllIcons.Duplicates.SendToTheLeftGrayed"  },
    ":": { "icon": "AllIcons.General.History", "description": "History"  },
    ",": { "icon": "AllIcons.Actions.SwapPanels" },
    "D": { "icon": "AllIcons.Providers.DocumentDB" },
    "[": { "icon": "AllIcons.Chooser.Left" },
    "]": { "icon": "AllIcons.Chooser.Right" },
    "/": { "icon":  "AllIcons.Toolwindows.Changes"},
    "p": { "icon": "AllIcons.Actions.DiffWithClipboard", "description": "Yank History" },
    "ac": { "icon": "AllIcons.Actions.Lightning", "description": "Open Claude" },
    "aC": { "icon": "AllIcons.Actions.Lightning", "description": "Send to Claude" },
    "ae": { "icon": "AllIcons.General.EditorPreviewVertical", "description": "AI Assistant - Ask in editor" },
    "cu": {"icon":  "AllIcons.General.History"}
  },
  "bindings": {
    "f": {
      "description": "File",
      "children": {
        "f": { "description": "Find File", "actionId": "GotoFile" },
        "r": { "description": "Recent Files", "actionId": "RecentFiles" },
        "s": { "description": "Save All", "actionId": "SaveAll" },
        "n": { "description": "New File", "actionId": "NewFile" },
        "e": { "description": "File Explorer", "actionId": "ActivateProjectToolWindow" }
      }
    },
    "b": {
      "description": "Buffers",
      "children": {
        "b": { "description": "Switch Buffer", "actionId": "Switcher" },
        "d": { "description": "Close Buffer", "actionId": "CloseContent" },
        "n": { "description": "Next Tab", "actionId": "NextTab" },
        "p": { "description": "Previous Tab", "actionId": "PreviousTab" }
      }
    },
    "c": {
      "description": "Code",
      "children": {
        "a": { "description": "Code Action", "actionId": "ShowIntentionActions" },
        "r": { "description": "Rename", "actionId": "RenameElement" },
        "f": { "description": "Format", "actionId": "ReformatCode" },
        "d": { "description": "Go to Definition", "actionId": "GotoDeclaration" },
        "i": { "description": "Go to Implementation", "actionId": "GotoImplementation" }
      }
    },
    "s": {
      "description": "Search",
      "children": {
        "s": { "description": "Search Text", "actionId": "FindInPath" },
        "r": { "description": "Replace in Files", "actionId": "ReplaceInPath" },
        "e": { "description": "Search Everywhere", "actionId": "SearchEverywhere" }
      }
    },
    "g": {
      "description": "Git",
      "children": {
        "s": { "description": "Status", "actionId": "Vcs.Show.Local.Changes" },
        "c": { "description": "Commit", "actionId": "CheckinProject" },
        "p": { "description": "Push", "actionId": "Vcs.Push" },
        "b": { "description": "Branches", "actionId": "Git.Branches" },
        "l": { "description": "Log", "actionId": "Vcs.Show.Log" }
      }
    },
    "w": {
      "description": "Window",
      "children": {
        "v": { "description": "Split Vertical", "actionId": "SplitVertically" },
        "h": { "description": "Split Horizontal", "actionId": "SplitHorizontally" },
        "q": { "description": "Close Window", "actionId": "CloseContent" },
        "o": { "description": "Close Others", "actionId": "CloseAllEditorsButActive" }
      }
    },
    "t": {
      "description": "Terminal",
      "children": {
        "t": { "description": "Toggle Terminal", "actionId": "ActivateTerminalToolWindow" }
      }
    },
    "q": { "description": "Quit", "actionId": "Exit" }
  }
}
""".trimIndent()
}
