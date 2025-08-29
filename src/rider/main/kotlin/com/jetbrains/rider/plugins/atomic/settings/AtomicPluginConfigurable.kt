package com.jetbrains.rider.plugins.atomic.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import com.intellij.ui.components.*
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.intellij.ui.TitledSeparator
import com.intellij.ui.JBColor
import com.intellij.ui.RoundedLineBorder
import com.intellij.ui.Gray
import com.intellij.ui.EditorNotifications
import java.awt.*
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import java.util.Hashtable

class AtomicPluginConfigurable(private val project: Project) : SearchableConfigurable {
    
    private var settingsComponent: AtomicPluginSettingsComponent? = null
    
    override fun getId(): String = "com.jetbrains.rider.plugins.atomic.settings"
    
    override fun getDisplayName(): String = "Atomic Plugin"
    
    override fun getHelpTopic(): String? = null
    
    override fun createComponent(): JComponent {
        settingsComponent = AtomicPluginSettingsComponent()
        return settingsComponent!!.panel
    }
    
    override fun isModified(): Boolean {
        val settings = AtomicPluginSettings.getInstance(project)
        val component = settingsComponent ?: return false
        
        return settings.autoGenerateEnabled != component.autoGenerateEnabled ||
               settings.showNotifications != component.showNotifications ||
               settings.debounceDelayMs != component.debounceDelayMs
    }
    
    override fun apply() {
        val settings = AtomicPluginSettings.getInstance(project)
        val component = settingsComponent ?: return
        
        settings.autoGenerateEnabled = component.autoGenerateEnabled
        settings.showNotifications = component.showNotifications
        settings.debounceDelayMs = component.debounceDelayMs
        
        // Refresh editor notifications to update the notification panel
        EditorNotifications.getInstance(project).updateAllNotifications()
    }
    
    override fun reset() {
        val settings = AtomicPluginSettings.getInstance(project)
        val component = settingsComponent ?: return
        
        component.autoGenerateEnabled = settings.autoGenerateEnabled
        component.showNotifications = settings.showNotifications
        component.debounceDelayMs = settings.debounceDelayMs
    }
    
    override fun disposeUIResources() {
        settingsComponent = null
    }
    
    private class AtomicPluginSettingsComponent {
        val panel: JPanel
        
        private val autoGenerateCheckBox = JBCheckBox("Enable auto-generation on file save")
        private val showNotificationsCheckBox = JBCheckBox("Show notifications after generation")
        private val debounceDelaySlider = JSlider(100, 5000, 500)
        private val debounceValueLabel = JBLabel("500 ms")
        private val debounceDelaySpinner = JSpinner(SpinnerNumberModel(500, 100, 5000, 50))
        
        var autoGenerateEnabled: Boolean
            get() = autoGenerateCheckBox.isSelected
            set(value) {
                autoGenerateCheckBox.isSelected = value
                updateAutoGenerateRelatedFields()
            }
        
        var showNotifications: Boolean
            get() = showNotificationsCheckBox.isSelected
            set(value) {
                showNotificationsCheckBox.isSelected = value
            }
        
        
        var debounceDelayMs: Long
            get() = (debounceDelaySpinner.value as Int).toLong()
            set(value) {
                debounceDelaySpinner.value = value.toInt()
                debounceDelaySlider.value = value.toInt()
                debounceValueLabel.text = "$value ms"
            }
        
        init {
            panel = createMainPanel()
            setupListeners()
            updateAutoGenerateRelatedFields()
        }
        
        private fun createMainPanel(): JPanel {
            val mainPanel = JPanel(BorderLayout())
            mainPanel.border = JBUI.Borders.empty(10)
            
            // Create scrollable content panel
            val contentPanel = JPanel()
            contentPanel.layout = BoxLayout(contentPanel, BoxLayout.Y_AXIS)
            contentPanel.border = JBUI.Borders.empty()
            
            // Add sections
            contentPanel.add(createHeaderSection())
            contentPanel.add(Box.createRigidArea(Dimension(0, 20)))
            contentPanel.add(createGenerationSection())
            contentPanel.add(Box.createRigidArea(Dimension(0, 20)))
            contentPanel.add(createQuickReferenceSection())
            contentPanel.add(Box.createVerticalGlue())
            
            // Wrap in scroll pane
            val scrollPane = JBScrollPane(contentPanel)
            scrollPane.border = null
            scrollPane.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
            scrollPane.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
            
            mainPanel.add(scrollPane, BorderLayout.CENTER)
            
            return mainPanel
        }
        
        private fun createHeaderSection(): JPanel {
            val panel = JPanel(BorderLayout())
            panel.maximumSize = Dimension(Integer.MAX_VALUE, 80)
            
            val headerPanel = JPanel(FlowLayout(FlowLayout.LEFT))
            headerPanel.background = UIUtil.getPanelBackground()
            
            // Plugin icon placeholder
            val iconLabel = JBLabel("🚀")
            iconLabel.font = iconLabel.font.deriveFont(24f)
            headerPanel.add(iconLabel)
            
            val textPanel = JPanel()
            textPanel.layout = BoxLayout(textPanel, BoxLayout.Y_AXIS)
            textPanel.background = UIUtil.getPanelBackground()
            
            val titleLabel = JBLabel("Atomic Plugin Settings")
            titleLabel.font = titleLabel.font.deriveFont(Font.BOLD, 16f)
            textPanel.add(titleLabel)
            
            val versionLabel = JBLabel("Version 0.1.5 - Code Generator for Atomic Framework")
            versionLabel.foreground = UIUtil.getContextHelpForeground()
            versionLabel.font = versionLabel.font.deriveFont(12f)
            textPanel.add(versionLabel)
            
            headerPanel.add(textPanel)
            panel.add(headerPanel, BorderLayout.WEST)
            
            return panel
        }
        
        private fun createGenerationSection(): JPanel {
            return createSection("Code Generation", listOf(
                createAutoGeneratePanel(),
                createDebounceDelayPanel(),
                createNotificationPanel()
            ))
        }
        
        private fun createAutoGeneratePanel(): JPanel {
            val panel = JPanel(BorderLayout())
            panel.border = JBUI.Borders.empty(5, 0)
            
            autoGenerateCheckBox.font = autoGenerateCheckBox.font.deriveFont(13f)
            panel.add(autoGenerateCheckBox, BorderLayout.NORTH)
            
            val descLabel = JBLabel("<html><span style='color: gray; font-size: 11px;'>Automatically regenerate code when .atomic files are saved. First-time generation always requires manual trigger (Ctrl+Shift+G).</span></html>")
            descLabel.border = JBUI.Borders.empty(2, 25, 0, 0)
            panel.add(descLabel, BorderLayout.CENTER)
            
            return panel
        }
        
        private fun createDebounceDelayPanel(): JPanel {
            val panel = JPanel(BorderLayout())
            panel.border = JBUI.Borders.empty(10, 25, 5, 0)
            
            // Title and value label
            val topPanel = JPanel(BorderLayout())
            val titleLabel = JBLabel("Debounce delay:")
            titleLabel.font = titleLabel.font.deriveFont(13f)
            topPanel.add(titleLabel, BorderLayout.WEST)
            
            debounceValueLabel.font = debounceValueLabel.font.deriveFont(Font.BOLD, 13f)
            debounceValueLabel.foreground = JBColor(Color(0, 120, 215), Color(100, 150, 255))
            topPanel.add(debounceValueLabel, BorderLayout.EAST)
            
            panel.add(topPanel, BorderLayout.NORTH)
            
            // Slider and spinner panel
            val controlPanel = JPanel(BorderLayout())
            controlPanel.border = JBUI.Borders.empty(5, 0)
            
            // Configure slider
            debounceDelaySlider.majorTickSpacing = 1000
            debounceDelaySlider.minorTickSpacing = 250
            debounceDelaySlider.paintTicks = true
            debounceDelaySlider.snapToTicks = false
            
            // Add labels to slider
            val labelTable = Hashtable<Int, JLabel>()
            labelTable[100] = JBLabel("100ms")
            labelTable[1000] = JBLabel("1s")
            labelTable[2500] = JBLabel("2.5s")
            labelTable[5000] = JBLabel("5s")
            debounceDelaySlider.labelTable = labelTable
            debounceDelaySlider.paintLabels = true
            debounceDelaySlider.font = debounceDelaySlider.font.deriveFont(10f)
            
            controlPanel.add(debounceDelaySlider, BorderLayout.CENTER)
            
            // Spinner for precise control
            val spinnerPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 0, 0))
            spinnerPanel.add(JBLabel("Precise: "))
            debounceDelaySpinner.preferredSize = Dimension(80, 25)
            spinnerPanel.add(debounceDelaySpinner)
            spinnerPanel.add(JBLabel(" ms"))
            
            controlPanel.add(spinnerPanel, BorderLayout.EAST)
            
            panel.add(controlPanel, BorderLayout.CENTER)
            
            // Description
            val descLabel = JBLabel("<html><span style='color: gray; font-size: 11px;'>Time to wait after file change before triggering auto-generation. Lower values are more responsive but may cause multiple regenerations.</span></html>")
            panel.add(descLabel, BorderLayout.SOUTH)
            
            return panel
        }
        
        private fun createNotificationPanel(): JPanel {
            val panel = JPanel(BorderLayout())
            panel.border = JBUI.Borders.empty(5, 0)
            
            showNotificationsCheckBox.font = showNotificationsCheckBox.font.deriveFont(13f)
            panel.add(showNotificationsCheckBox, BorderLayout.NORTH)
            
            val descLabel = JBLabel("<html><span style='color: gray; font-size: 11px;'>Display success and error notifications in the IDE after code generation completes.</span></html>")
            descLabel.border = JBUI.Borders.empty(2, 25, 0, 0)
            panel.add(descLabel, BorderLayout.CENTER)
            
            return panel
        }
        
        private fun createQuickReferenceSection(): JPanel {
            val section = createSection("Quick Reference", emptyList())
            
            val content = JPanel()
            content.layout = BoxLayout(content, BoxLayout.Y_AXIS)
            content.border = JBUI.Borders.empty(5, 10)
            
            // Keyboard shortcuts
            val shortcutsPanel = createInfoPanel(
                "⌨️ Keyboard Shortcuts",
                listOf(
                    "<b>Ctrl+Shift+G</b> - Generate/Regenerate code from .atomic file",
                    "<b>Alt+Enter</b> - Show quick fixes in .atomic files",
                    "<b>Ctrl+Click</b> - Navigate to generated code"
                )
            )
            content.add(shortcutsPanel)
            content.add(Box.createRigidArea(Dimension(0, 10)))
            
            // File format example
            val formatPanel = createCodeExamplePanel()
            content.add(formatPanel)
            
            section.add(content, BorderLayout.CENTER)
            return section
        }
        
        private fun createSection(title: String, components: List<JComponent>): JPanel {
            val panel = JPanel(BorderLayout())
            panel.maximumSize = Dimension(Integer.MAX_VALUE, panel.preferredSize.height)
            
            // Section header
            val separator = TitledSeparator(title)
            separator.font = separator.font.deriveFont(Font.BOLD, 14f)
            panel.add(separator, BorderLayout.NORTH)
            
            // Section content
            val contentPanel = JPanel()
            contentPanel.layout = BoxLayout(contentPanel, BoxLayout.Y_AXIS)
            contentPanel.border = JBUI.Borders.empty(10, 20, 10, 20)
            
            for (component in components) {
                contentPanel.add(component)
            }
            
            panel.add(contentPanel, BorderLayout.CENTER)
            
            return panel
        }
        
        private fun createInfoPanel(title: String, items: List<String>): JPanel {
            val panel = JPanel(BorderLayout())
            panel.background = UIUtil.getPanelBackground()
            panel.border = RoundedLineBorder(Gray._200, 8, 1)
            
            val contentPanel = JPanel()
            contentPanel.layout = BoxLayout(contentPanel, BoxLayout.Y_AXIS)
            contentPanel.border = JBUI.Borders.empty(10)
            contentPanel.background = UIUtil.getPanelBackground()
            
            val titleLabel = JBLabel(title)
            titleLabel.font = titleLabel.font.deriveFont(Font.BOLD, 12f)
            contentPanel.add(titleLabel)
            contentPanel.add(Box.createRigidArea(Dimension(0, 5)))
            
            for (item in items) {
                val itemLabel = JBLabel("<html>• $item</html>")
                itemLabel.font = itemLabel.font.deriveFont(11f)
                itemLabel.border = JBUI.Borders.empty(2, 10, 2, 0)
                contentPanel.add(itemLabel)
            }
            
            panel.add(contentPanel, BorderLayout.CENTER)
            return panel
        }
        
        private fun createCodeExamplePanel(): JPanel {
            val panel = JPanel(BorderLayout())
            panel.background = JBColor(Color(245, 245, 245), Color(45, 45, 45))
            panel.border = RoundedLineBorder(Gray._200, 8, 1)
            
            val contentPanel = JPanel(BorderLayout())
            contentPanel.border = JBUI.Borders.empty(10)
            contentPanel.background = panel.background
            
            val titleLabel = JBLabel("📄 Example .atomic file:")
            titleLabel.font = titleLabel.font.deriveFont(Font.BOLD, 12f)
            contentPanel.add(titleLabel, BorderLayout.NORTH)
            
            val codeArea = JTextArea("""
namespace: MyGame.Components
className: EntityExtensions
entityType: IEntity

imports:
  - UnityEngine
  - System.Collections.Generic

tags:
  - Player
  - Enemy

values:
  - Health: int
  - Position: Vector3
  - Inventory: List<Item>
            """.trimIndent())
            
            codeArea.isEditable = false
            codeArea.font = Font(Font.MONOSPACED, Font.PLAIN, 11)
            codeArea.background = panel.background
            codeArea.foreground = UIUtil.getLabelForeground()
            codeArea.border = JBUI.Borders.empty(5, 10)
            
            contentPanel.add(codeArea, BorderLayout.CENTER)
            panel.add(contentPanel, BorderLayout.CENTER)
            
            return panel
        }
        
        private fun setupListeners() {
            // Auto-generate checkbox listener
            autoGenerateCheckBox.addActionListener {
                updateAutoGenerateRelatedFields()
            }
            
            // Sync slider and spinner
            debounceDelaySlider.addChangeListener {
                if (!debounceDelaySlider.valueIsAdjusting) {
                    val value = debounceDelaySlider.value
                    debounceDelaySpinner.value = value
                    debounceValueLabel.text = "$value ms"
                }
            }
            
            debounceDelaySpinner.addChangeListener {
                val value = debounceDelaySpinner.value as Int
                debounceDelaySlider.value = value
                debounceValueLabel.text = "$value ms"
            }
        }
        
        private fun updateAutoGenerateRelatedFields() {
            val enabled = autoGenerateCheckBox.isSelected
            debounceDelaySlider.isEnabled = enabled
            debounceDelaySpinner.isEnabled = enabled
            debounceValueLabel.isEnabled = enabled
        }
    }
}