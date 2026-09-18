package lazyideavim.whichkeylazy.ui

import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import java.awt.Color
import java.awt.Font
import javax.swing.UIManager

/**
 * Theme-aware colors derived from JetBrains popup and editor themes.
 */
object WhichKeyColors {

    private fun scheme() = EditorColorsManager.getInstance().schemeForCurrentUITheme

    val PANEL_BG: Color get() = JBUI.CurrentTheme.Popup.BACKGROUND

    val DESC_FG: Color
        get() = JBColor.namedColor("Popup.foreground", scheme().defaultForeground)

    val KEY_BG: Color
        get() {
            val fallback = blendColors(DESC_FG, PANEL_BG, if (JBColor.isBright()) 0.9f else 0.84f)
            return JBColor.namedColor("CompletionPopup.selectionInactiveBackground", fallback)
        }

    val KEY_FG: Color
        get() {
            val keyword = scheme().getAttributes(
                TextAttributesKey.createTextAttributesKey("DEFAULT_KEYWORD")
            )
            return keyword?.foregroundColor ?: scheme().defaultForeground
        }

    val GROUP_FG: Color get() = KEY_FG

    val SEPARATOR: Color get() = JBUI.CurrentTheme.Popup.separatorColor()

    val BORDER: Color get() = JBUI.CurrentTheme.Popup.borderColor(true)

    val BREADCRUMB: Color get() = JBUI.CurrentTheme.Popup.headerForeground(true)

    val UI_FONT: Font
        get() = UIManager.getFont("Label.font") ?: scheme().getFont(EditorFontType.PLAIN)

    val UI_FONT_SIZE: Float get() = UI_FONT.size2D

    private fun blendColors(c1: Color, c2: Color, ratio: Float): Color {
        val r = (c1.red * (1 - ratio) + c2.red * ratio).toInt().coerceIn(0, 255)
        val g = (c1.green * (1 - ratio) + c2.green * ratio).toInt().coerceIn(0, 255)
        val b = (c1.blue * (1 - ratio) + c2.blue * ratio).toInt().coerceIn(0, 255)
        return Color(r, g, b)
    }
}
