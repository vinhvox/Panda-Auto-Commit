package com.viotech.auto

import com.intellij.openapi.options.Configurable
import com.intellij.util.ui.FormBuilder
import javax.swing.*

class GeminiSettingsConfigurable : Configurable {
    private var mainPanel: JPanel? = null
    private val apiKeyField = JPasswordField()
    private val useCustomRoleCheckbox = JCheckBox("Use Custom Roles (Custom Prompts)")
    private val customRoleArea = JTextArea(5, 40)

    private val defaultRoleText = "You are an expert developer. Write a concise Conventional Commit message based on the provided git diff. Do NOT wrap the response in markdown blocks like ```. Just return the raw text."

    override fun getDisplayName(): String = "Panda Commit Auto"

    override fun createComponent(): JComponent {
        customRoleArea.lineWrap = true
        customRoleArea.wrapStyleWord = true
        val scrollPane = JScrollPane(customRoleArea)

        useCustomRoleCheckbox.addActionListener {
            customRoleArea.isEnabled = useCustomRoleCheckbox.isSelected
            if (!useCustomRoleCheckbox.isSelected) {
                customRoleArea.text = defaultRoleText
            }
        }

        mainPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JLabel("Gemini API Key: "), apiKeyField, 1, false)
            .addComponent(useCustomRoleCheckbox, 1)
            .addLabeledComponent(JLabel("Custom Role Prompt: "), scrollPane, 1, true)
            .addComponentFillVertically(JPanel(), 0)
            .panel

        return mainPanel!!
    }

    override fun isModified(): Boolean {
        val settings = GeminiSettingsState.instance
        return String(apiKeyField.password) != settings.apiKey ||
                useCustomRoleCheckbox.isSelected != settings.useCustomRole ||
                customRoleArea.text != settings.customRoleText
    }

    override fun apply() {
        val settings = GeminiSettingsState.instance
        settings.apiKey = String(apiKeyField.password)
        settings.useCustomRole = useCustomRoleCheckbox.isSelected
        settings.customRoleText = customRoleArea.text
    }

    override fun reset() {
        val settings = GeminiSettingsState.instance
        apiKeyField.text = settings.apiKey
        useCustomRoleCheckbox.isSelected = settings.useCustomRole
        customRoleArea.text = if (settings.useCustomRole) settings.customRoleText else defaultRoleText
        customRoleArea.isEnabled = settings.useCustomRole
    }
}