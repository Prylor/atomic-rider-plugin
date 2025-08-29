package com.jetbrains.rider.plugins.atomic.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
@State(
    name = "AtomicPluginSettings",
    storages = [Storage("atomicPlugin.xml")]
)
class AtomicPluginSettings : PersistentStateComponent<AtomicPluginSettings.State> {
    
    data class State(
        var autoGenerateEnabled: Boolean = true,
        var showNotifications: Boolean = true,
        var debounceDelayMs: Long = 500L
    )
    
    private var myState = State()
    
    override fun getState(): State = myState
    
    override fun loadState(state: State) {
        myState = state
    }
    
    companion object {
        fun getInstance(project: Project): AtomicPluginSettings {
            return project.getService(AtomicPluginSettings::class.java)
        }
    }
    
    var autoGenerateEnabled: Boolean
        get() = myState.autoGenerateEnabled
        set(value) {
            myState.autoGenerateEnabled = value
        }
    
    var showNotifications: Boolean
        get() = myState.showNotifications
        set(value) {
            myState.showNotifications = value
        }
    
    var debounceDelayMs: Long
        get() = myState.debounceDelayMs
        set(value) {
            myState.debounceDelayMs = value
        }
}