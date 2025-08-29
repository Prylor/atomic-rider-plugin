package com.jetbrains.rider.plugins.atomic.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.jetbrains.rider.plugins.atomic.language.AtomicFileType
import com.jetbrains.rider.plugins.atomic.psi.AtomicFile
import com.jetbrains.rider.plugins.atomic.services.AtomicAutoGenerator
import com.jetbrains.rider.plugins.atomic.services.AtomicGenerationService
import kotlinx.coroutines.runBlocking

class AtomicGenerateApiAction : AnAction("Generate Entity API", "Generate C# API from .atomic file", null) {
    companion object {
        private val logger = Logger.getInstance(AtomicGenerateApiAction::class.java)
    }
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        
        if (virtualFile.fileType != AtomicFileType) {
            Messages.showErrorDialog(project, "This action can only be performed on .atomic files", "Invalid File Type")
            return
        }
        
        FileDocumentManager.getInstance().saveAllDocuments()
        
        val psiFile = PsiManager.getInstance(project).findFile(virtualFile) as? AtomicFile
        if (psiFile == null) {
            Messages.showErrorDialog(project, "Could not parse .atomic file", "Parse Error")
            return
        }
        
        // Use AtomicAutoGenerator for manual generation with forceCreate = true
        val autoGenerator = AtomicAutoGenerator.getInstance(project)
        autoGenerator.generateManually(psiFile)
        
        // Show success message
        val headerProperties = parseHeaderProperties(psiFile)
        val className = headerProperties["className"] ?: "EntityApi"
        val directory = headerProperties["directory"] ?: ""
        
        Messages.showInfoMessage(
            project,
            "Generation started for ${className}.cs${if (directory.isNotEmpty()) " in $directory" else ""}.\n\nCheck notifications for result.",
            "Generation Started"
        )
    }
    
    override fun update(e: AnActionEvent) {
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = virtualFile?.fileType == AtomicFileType
    }
    
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
    
    private fun parseHeaderProperties(atomicFile: AtomicFile): Map<String, String> {
        val properties = mutableMapOf<String, String>()
        
        atomicFile.node.text.lines().forEach { line ->
            when {
                line.trim().startsWith("namespace:") -> {
                    properties["namespace"] = line.substringAfter("namespace:").trim().trim('"')
                }
                line.trim().startsWith("className:") -> {
                    properties["className"] = line.substringAfter("className:").trim().trim('"')
                }
                line.trim().startsWith("directory:") -> {
                    properties["directory"] = line.substringAfter("directory:").trim().trim('"')
                }
            }
        }
        
        return properties
    }
}