package com.viotech.auto

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "PandaCommitSettings",
    storages = [Storage("PandaCommitSettings.xml")]
)
class GeminiSettingsState : PersistentStateComponent<GeminiSettingsState> {
    var apiKey: String = ""
    var useCustomRole: Boolean = false
    var customRoleText: String = ""

    override fun getState(): GeminiSettingsState = this

    override fun loadState(state: GeminiSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        val instance: GeminiSettingsState
            get() = ApplicationManager.getApplication().getService(GeminiSettingsState::class.java)
    }
}