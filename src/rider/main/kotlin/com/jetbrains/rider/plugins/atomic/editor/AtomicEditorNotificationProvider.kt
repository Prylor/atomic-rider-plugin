package com.jetbrains.rider.plugins.atomic.editor

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import com.jetbrains.rider.plugins.atomic.language.AtomicFileType
import com.jetbrains.rider.plugins.atomic.psi.*
import com.jetbrains.rider.plugins.atomic.psi.impl.AtomicPsiImplUtil
import com.jetbrains.rider.plugins.atomic.services.AtomicAutoGenerator
import com.jetbrains.rider.plugins.atomic.services.AtomicFileValidator
import com.jetbrains.rider.plugins.atomic.services.GeneratedFileTracker
import com.jetbrains.rider.plugins.atomic.settings.AtomicPluginSettings
import java.io.File
import java.nio.file.Paths
import java.util.function.Function
import javax.swing.JComponent

class AtomicEditorNotificationProvider : EditorNotificationProvider {
    
    override fun collectNotificationData(
        project: Project,
        file: VirtualFile
    ): Function<in FileEditor, out JComponent?>? {
        if (file.fileType != AtomicFileType) {
            return null
        }
        
        return Function { fileEditor ->
            val psiFile = PsiManager.getInstance(project).findFile(file) as? AtomicFile
                ?: return@Function null
            
            val validator = AtomicFileValidator.getInstance(project)
            val tracker = GeneratedFileTracker.getInstance(project)
            val autoGenerator = AtomicAutoGenerator.getInstance(project)
            val settings = AtomicPluginSettings.getInstance(project)
            
            val hasErrors = validator.hasErrors(psiFile)
            if (hasErrors) {
                val panel = EditorNotificationPanel(fileEditor, EditorNotificationPanel.Status.Error)
                panel.text = "Fix validation errors before generating API"
                return@Function panel
            }
            
            val calculatedPath = calculateGeneratedFilePath(project, file, psiFile)
            
            val generatedFileInfo = tracker.getGeneratedFileInfo(file)
            val trackedPath = generatedFileInfo?.generatedFilePath
            
            val generatedFilePath = trackedPath ?: calculatedPath
            
            val generatedFileExists = generatedFilePath?.let {
                try {
                    File(it).exists()
                } catch (e: Exception) {
                    false
                }
            } ?: false
            
            val panel = EditorNotificationPanel(fileEditor, EditorNotificationPanel.Status.Info)
            
            if (generatedFileExists) {
                val autoGenStatus = if (settings.autoGenerateEnabled) {
                    "Auto-generation enabled - file will update on save"
                } else {
                    "Auto-generation disabled - use manual generation"
                }
                panel.text = "API file exists. $autoGenStatus. Click to regenerate manually or use Ctrl+Shift+G"
                panel.createActionLabel("Regenerate API") {
                    autoGenerator.generateManually(psiFile)
                }
            } else {
                val autoGenInfo = if (settings.autoGenerateEnabled) {
                    "Auto-generation will start after first manual generation"
                } else {
                    "Auto-generation is disabled in settings"
                }
                panel.text = "No generated API file found. $autoGenInfo. Click to generate or use Ctrl+Shift+G"
                panel.createActionLabel("Generate API") {
                    autoGenerator.generateManually(psiFile)
                }
            }
            
            // Add settings link if auto-generation is disabled
            if (!settings.autoGenerateEnabled) {
                panel.createActionLabel("Configure Settings") {
                    com.intellij.openapi.options.ShowSettingsUtil.getInstance().showSettingsDialog(
                        project,
                        "Atomic Plugin"
                    )
                }
            }
            
            if (generatedFileExists && generatedFilePath != null) {
                panel.createActionLabel("Open Generated File") {
                    try {
                        val generatedFile = File(generatedFilePath)
                        if (generatedFile.exists()) {
                            val virtualFile = VirtualFileManager.getInstance()
                                .refreshAndFindFileByNioPath(Paths.get(generatedFilePath))
                            
                            virtualFile?.let {
                                FileEditorManager.getInstance(project).openFile(it, true)
                            }
                        }
                    } catch (e: Exception) {
                    }
                }
                
                panel.createActionLabel("Delete Generated File") {
                    try {
                        val generatedFile = File(generatedFilePath)
                        if (generatedFile.exists()) {
                            val result = com.intellij.openapi.ui.Messages.showYesNoDialog(
                                project,
                                "Are you sure you want to delete the generated file?\n$generatedFilePath",
                                "Delete Generated File",
                                com.intellij.openapi.ui.Messages.getWarningIcon()
                            )
                            
                            if (result == com.intellij.openapi.ui.Messages.YES) {
                                if (generatedFile.delete()) {
                                    tracker.removeTracking(file)
                                    
                                    VirtualFileManager.getInstance().refreshAndFindFileByNioPath(Paths.get(generatedFilePath))?.let { vf ->
                                        vf.refresh(false, false)
                                    }
                                    
                                    FileEditorManager.getInstance(project).selectedTextEditor?.let { editor ->
                                        com.intellij.ui.EditorNotifications.getInstance(project).updateNotifications(file)
                                    }
                                    
                                    com.intellij.openapi.ui.Messages.showInfoMessage(
                                        project,
                                        "Generated file deleted successfully",
                                        "File Deleted"
                                    )
                                } else {
                                    com.intellij.openapi.ui.Messages.showErrorDialog(
                                        project,
                                        "Failed to delete the generated file",
                                        "Delete Failed"
                                    )
                                }
                            }
                        }
                    } catch (e: Exception) {
                        com.intellij.openapi.ui.Messages.showErrorDialog(
                            project,
                            "Error deleting file: ${e.message}",
                            "Delete Error"
                        )
                    }
                }
            }
            
            panel
        }
    }
    
    private fun calculateGeneratedFilePath(project: Project, atomicFile: VirtualFile, psiFile: AtomicFile): String? {
        val projectBasePath = project.basePath ?: return null
        
        var className: String? = null
        var directory: String? = null
        
        // Parse header section
        psiFile.children.filterIsInstance<AtomicHeaderSection>().forEach { headerSection ->
            headerSection.children.forEach { child ->
            }
        }
        
        fun parseElement(element: PsiElement) {
            element.children.forEach { child ->
                when (child) {
                    is AtomicClassNameProp -> className = AtomicPsiImplUtil.getValue(child)
                    is AtomicDirectoryProp -> directory = AtomicPsiImplUtil.getValue(child)
                }
                parseElement(child)
            }
        }
        parseElement(psiFile)
        
        if (className == null || directory == null) {
            return null
        }
        
        val outputDir = if (File(directory!!).isAbsolute) {
            File(directory!!)
        } else {
            File(projectBasePath, directory!!)
        }
        
        return File(outputDir, "$className.cs").absolutePath
    }
}