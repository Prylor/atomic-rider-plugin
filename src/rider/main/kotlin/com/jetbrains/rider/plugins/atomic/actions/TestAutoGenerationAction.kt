package com.jetbrains.rider.plugins.atomic.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiManager
import com.jetbrains.rider.plugins.atomic.psi.AtomicFile
import com.jetbrains.rider.plugins.atomic.services.AtomicAutoGenerator
import com.jetbrains.rider.plugins.atomic.settings.AtomicPluginSettings
import kotlinx.coroutines.runBlocking

/**
 * Test action to debug auto-generation
 */
class TestAutoGenerationAction : AnAction("Test Auto Generation") {
    
    override fun getActionUpdateThread() = com.intellij.openapi.actionSystem.ActionUpdateThread.BGT
    
    companion object {
        private val LOG = Logger.getInstance(TestAutoGenerationAction::class.java)
    }
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        
        if (file.extension != "atomic") {
            LOG.info("Not an atomic file: ${file.path}")
            return
        }
        
        LOG.info("Testing auto-generation for: ${file.path}")
        
        
        val settings = AtomicPluginSettings.getInstance(project)
        LOG.info("Settings - autoGenerateEnabled: ${settings.autoGenerateEnabled}")
        LOG.info("Settings - showNotifications: ${settings.showNotifications}")
        LOG.info("Settings - debounceDelayMs: ${settings.debounceDelayMs}")
        
        
        val psiFile = PsiManager.getInstance(project).findFile(file) as? AtomicFile
        if (psiFile == null) {
            LOG.error("Could not get PSI file")
            return
        }
        
        LOG.info("Got PSI file: ${psiFile.name}")
        
        
        val autoGenerator = AtomicAutoGenerator.getInstance(project)
        runBlocking {
            autoGenerator.regenerateIfValid(psiFile)
        }
        
        LOG.info("Auto-generation test completed")
    }
    
    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = file?.extension == "atomic"
    }
}