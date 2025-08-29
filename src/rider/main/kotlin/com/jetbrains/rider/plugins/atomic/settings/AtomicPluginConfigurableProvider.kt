package com.jetbrains.rider.plugins.atomic.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurableProvider
import com.intellij.openapi.project.Project

class AtomicPluginConfigurableProvider(private val project: Project) : ConfigurableProvider() {
    
    override fun createConfigurable(): Configurable {
        return AtomicPluginConfigurable(project)
    }
    
    override fun canCreateConfigurable(): Boolean {
        return true
    }
}