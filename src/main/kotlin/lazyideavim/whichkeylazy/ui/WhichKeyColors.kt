package lazyideavim.whichkeylazy.ui

import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.ui.JBColor
import java.awt.Color
import java.awt.Font

/**
 * Theme-aware colors derived from the current editor color scheme.
 */
object WhichKeyColors {

    private fun scheme() = EditorColorsManager.getInstance().schemeForCurrentUITheme

    val PANEL_BG: Color get() = scheme().defaultBackground

    val DESC_FG: Color get() = scheme().defaultForeground

    val KEY_BG: Color
        get() {
            val bg = PANEL_BG
            return if (JBColor.isBright()) bg.darker() else bg.brighter()
        }

    val KEY_FG: Color
        get() {
            val keyword = scheme().getAttributes(
                TextAttributesKey.createTextAttributesKey("DEFAULT_KEYWORD")
            )
            return keyword?.foregroundColor ?: scheme().defaultForeground
        }

    val GROUP_FG: Color get() = KEY_FG

    val SEPARATOR: Color
        get() = blendColors(scheme().defaultForeground, scheme().defaultBackground, 0.5f)

    val BORDER: Color
        get() {
            val bg = PANEL_BG
            return if (JBColor.isBright()) blendColors(bg, Color.BLACK, 0.15f)
            else blendColors(bg, Color.WHITE, 0.15f)
        }

    val BREADCRUMB: Color get() = KEY_FG

    val EDITOR_FONT: Font get() = scheme().getFont(EditorFontType.PLAIN)

    val EDITOR_FONT_SIZE: Int get() = scheme().editorFontSize

    private fun blendColors(c1: Color, c2: Color, ratio: Float): Color {
        val r = (c1.red * (1 - ratio) + c2.red * ratio).toInt().coerceIn(0, 255)
        val g = (c1.green * (1 - ratio) + c2.green * ratio).toInt().coerceIn(0, 255)
        val b = (c1.blue * (1 - ratio) + c2.blue * ratio).toInt().coerceIn(0, 255)
        return Color(r, g, b)
    }
}
