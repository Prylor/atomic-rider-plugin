package com.jetbrains.rider.plugins.atomic.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiManager
import com.jetbrains.rider.plugins.atomic.psi.AtomicFile
import com.jetbrains.rider.plugins.atomic.services.AtomicAutoGenerator
import com.jetbrains.rider.plugins.atomic.services.AtomicGenerationService
import com.jetbrains.rider.plugins.atomic.services.GeneratedFileTracker
import com.jetbrains.rider.plugins.atomic.settings.AtomicPluginSettings

/**
 * Diagnostic action to help debug auto-generation issues
 */
class DiagnoseAutoGenerationAction : AnAction("Diagnose Auto Generation") {
    
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
    
    companion object {
        private val LOG = Logger.getInstance(DiagnoseAutoGenerationAction::class.java)
    }
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        
        if (file.extension != "atomic") {
            Messages.showInfoMessage("Please select an .atomic file", "Diagnose Auto Generation")
            return
        }
        
        val diagnostics = StringBuilder()
        diagnostics.appendLine("=== Auto Generation Diagnostics ===")
        diagnostics.appendLine("File: ${file.path}")
        diagnostics.appendLine()
        
        
        val settings = AtomicPluginSettings.getInstance(project)
        diagnostics.appendLine("Settings:")
        diagnostics.appendLine("  Auto-generate enabled: ${settings.autoGenerateEnabled}")
        diagnostics.appendLine("  Show notifications: ${settings.showNotifications}")
        diagnostics.appendLine("  Debounce delay: ${settings.debounceDelayMs}ms")
        diagnostics.appendLine()
        
        
        val psiFile = PsiManager.getInstance(project).findFile(file) as? AtomicFile
        diagnostics.appendLine("PSI Status:")
        diagnostics.appendLine("  PSI file found: ${psiFile != null}")
        if (psiFile != null) {
            diagnostics.appendLine("  PSI file name: ${psiFile.name}")
            diagnostics.appendLine("  PSI file valid: ${psiFile.isValid}")
        }
        diagnostics.appendLine()
        
        
        val generationService = AtomicGenerationService.getInstance(project)
        diagnostics.appendLine("Generation Service:")
        diagnostics.appendLine("  Service ready: ${generationService.isReady()}")
        diagnostics.appendLine()
        
        
        val fileTracker = GeneratedFileTracker.getInstance(project)
        val trackedInfo = fileTracker.getGeneratedFileInfo(file)
        diagnostics.appendLine("File Tracking:")
        if (trackedInfo != null) {
            diagnostics.appendLine("  Tracked: Yes")
            diagnostics.appendLine("  Generated file: ${trackedInfo.generatedFilePath}")
            diagnostics.appendLine("  Directory: ${trackedInfo.directory}")
            diagnostics.appendLine("  Class name: ${trackedInfo.className}")
        } else {
            diagnostics.appendLine("  Tracked: No")
        }
        diagnostics.appendLine()
        
        
        diagnostics.appendLine("File System:")
        diagnostics.appendLine("  File exists: ${file.exists()}")
        diagnostics.appendLine("  File writable: ${file.isWritable}")
        diagnostics.appendLine("  File in local FS: ${file.isInLocalFileSystem}")
        diagnostics.appendLine()
        
        
        val message = diagnostics.toString()
        LOG.info(message)
        
        Messages.showMultilineInputDialog(
            project,
            message,
            "Auto Generation Diagnostics",
            message,
            Messages.getInformationIcon(),
            null
        )
    }
    
    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = file?.extension == "atomic"
    }
}