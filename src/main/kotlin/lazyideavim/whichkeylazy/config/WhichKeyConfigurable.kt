package lazyideavim.whichkeylazy.config

import lazyideavim.whichkeylazy.model.PopupPlacement
import lazyideavim.whichkeylazy.model.WhichKeySettings
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.ui.JBIntSpinner
import com.intellij.util.ui.FormBuilder
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Settings page under Tools for popup placement and padding.
 *
 * Values are written back to `~/.whichkey-lazy.json` so the file remains
 * the single source of truth for plugin configuration.
 */
class WhichKeyConfigurable : Configurable {

    private var panel: JPanel? = null
    private var placementCombo: JComboBox<PopupPlacement>? = null
    private var gapXSpinner: JBIntSpinner? = null
    private var gapYSpinner: JBIntSpinner? = null
    private var originalSettings: WhichKeySettings = WhichKeySettings()

    override fun getDisplayName(): String = "Which Key Lazy"

    override fun createComponent(): JComponent {
        val combo = JComboBox(PopupPlacement.entries.toTypedArray())
        val gapX = JBIntSpinner(0, 0, MAX_GAP)
        val gapY = JBIntSpinner(0, 0, MAX_GAP)

        placementCombo = combo
        gapXSpinner = gapX
        gapYSpinner = gapY

        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent("Placement:", combo)
            .addLabeledComponent("Gap X (px):", gapX)
            .addLabeledComponent("Gap Y (px):", gapY)
            .addComponentFillVertically(JPanel(), 0)
            .panel

        reset()
        return panel!!
    }

    override fun isModified(): Boolean {
        val placement = selectedPlacement().id
        return placement != originalSettings.placement ||
            currentGapX() != originalSettings.gapX ||
            currentGapY() != originalSettings.gapY
    }

    override fun apply() {
        val updated = originalSettings.copy(
            placement = selectedPlacement().id,
            gapX = currentGapX(),
            gapY = currentGapY()
        )
        WhichKeyConfigService.getInstance().saveSettings(updated).onFailure {
            throw ConfigurationException("Could not save Which Key Lazy settings: ${it.message}")
        }
        originalSettings = updated
    }

    override fun reset() {
        originalSettings = WhichKeyConfigService.getInstance().settings
        placementCombo?.selectedItem = PopupPlacement.fromId(originalSettings.placement)
        gapXSpinner?.number = originalSettings.gapX
        gapYSpinner?.number = originalSettings.gapY
    }

    override fun disposeUIResources() {
        panel = null
        placementCombo = null
        gapXSpinner = null
        gapYSpinner = null
    }

    private fun selectedPlacement(): PopupPlacement =
        placementCombo?.selectedItem as? PopupPlacement ?: PopupPlacement.DEFAULT

    private fun currentGapX(): Int = gapXSpinner?.number ?: 0

    private fun currentGapY(): Int = gapYSpinner?.number ?: 0

    companion object {
        private const val MAX_GAP = 1000
    }
}
