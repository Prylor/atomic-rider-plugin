package com.jetbrains.rider.plugins.atomic.actions

import com.intellij.ide.actions.CreateElementActionBase
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.jetbrains.rider.plugins.atomic.dialogs.CreateAtomicFileDialog
import com.jetbrains.rider.plugins.atomic.language.AtomicFileType
import com.jetbrains.rider.plugins.atomic.language.AtomicIcons
import com.jetbrains.rider.plugins.atomic.services.AtomicGenerationService
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class CreateAtomicFileAction : CreateElementActionBase(
    "Atomic File",
    "Create new Atomic configuration file",
    AtomicIcons.FILE
), DumbAware {
    
    companion object {
        private val LOG = Logger.getInstance(CreateAtomicFileAction::class.java)
    }
    
    override fun invokeDialog(project: Project, directory: PsiDirectory): Array<PsiElement> {
        val dialog = CreateAtomicFileDialog(project, directory.virtualFile.path)
        
        if (!dialog.showAndGet()) {
            return PsiElement.EMPTY_ARRAY
        }
        
        val result = dialog.getResult()
        
        
        val content = buildString {
            if (!result.header.isNullOrBlank()) {
                appendLine("header: \"${result.header}\"")
            }
            
            if (!result.namespace.isNullOrBlank()) {
                appendLine("namespace: ${result.namespace}")
            }
            appendLine("className: ${result.className}")
            if (!result.directory.isNullOrBlank()) {
                
                val directoryPath = processDirectoryPath(result.directory, project)
                if (directoryPath.isNotBlank()) {
                    appendLine("directory: $directoryPath")
                }
            }
            appendLine("aggressiveInlining: ${result.aggressiveInlining}")
            appendLine("unsafe: ${result.unsafe}")
            if (!result.entityType.isNullOrBlank()) {
                appendLine("entityType: ${result.entityType}")
            }

            appendLine()
            
            
            appendLine("imports:")
            appendLine()
            appendLine("tags:")
            appendLine("  # Add your tags here")
            appendLine("  # - Player")
            appendLine("  # - Enemy")
            appendLine()
            appendLine("values:")
            appendLine("  # Add your values here")
            appendLine("  # - health: float")
            appendLine("  # - position: Vector3")
        }
        
        
        val file = runWriteAction {
            val psiFile = PsiFileFactory.getInstance(project).createFileFromText(
                result.fileName,
                AtomicFileType,
                content
            )
            
            directory.add(psiFile) as PsiFile
        }
        
        
        if (file != null) {
            val virtualFile = file.virtualFile
            
            if (virtualFile != null) {
                ApplicationManager.getApplication().invokeLater {
                    GlobalScope.launch {
                        try {
                            val generationService = AtomicGenerationService.getInstance(project)
                            val success = generationService.addAtomicFileToProject(virtualFile.path)
                            
                            if (success) {
                                LOG.info("Successfully added atomic file to project: ${virtualFile.path}")
                            } else {
                                LOG.warn("Failed to add atomic file to project: ${virtualFile.path}")
                            }
                        } catch (e: Exception) {
                            LOG.error("Error adding atomic file to project", e)
                        }
                    }
                }
            }
        }
        
        return arrayOf(file)
    }
    
    override fun create(newName: String, directory: PsiDirectory): Array<PsiElement> {
        
        return PsiElement.EMPTY_ARRAY
    }
    
    override fun getErrorTitle(): String {
        return "Cannot Create Atomic File"
    }
    
    override fun getCommandName(): String {
        return "Create Atomic File"
    }
    
    override fun getActionName(directory: PsiDirectory, newName: String): String {
        return "Create Atomic File"
    }
    
    override fun isAvailable(dataContext: DataContext): Boolean {
        val project = CommonDataKeys.PROJECT.getData(dataContext) ?: return false
        val directory = CommonDataKeys.PSI_ELEMENT.getData(dataContext) as? PsiDirectory
        return directory != null && super.isAvailable(dataContext)
    }
    
    private fun processDirectoryPath(inputPath: String, project: Project): String {
        val projectBasePath = project.basePath ?: return ""
        val normalizedInput = inputPath.replace('\\', '/')
        
        try {
            val projectRoot = java.nio.file.Paths.get(projectBasePath)
            val inputPathObj = java.nio.file.Paths.get(normalizedInput)
            
            
            when {
                
                !inputPathObj.isAbsolute -> {
                    
                    var result = normalizedInput
                    if (result == "." || result.isEmpty()) {
                        return ""
                    }
                    
                    if (!result.endsWith("/")) {
                        result += "/"
                    }
                    return result
                }
                
                
                inputPathObj.startsWith(projectRoot) -> {
                    val relativePath = projectRoot.relativize(inputPathObj).toString().replace('\\', '/')
                    if (relativePath.isEmpty()) {
                        return ""
                    }
                    
                    return if (relativePath.endsWith("/")) relativePath else "$relativePath/"
                }
                
                
                else -> {
                    LOG.warn("Selected directory is outside project: $normalizedInput")
                    
                    val assetsIndex = normalizedInput.lastIndexOf("/Assets/")
                    if (assetsIndex >= 0) {
                        var result = normalizedInput.substring(assetsIndex + 1) 
                        if (!result.endsWith("/")) {
                            result += "/"
                        }
                        return result
                    }
                    
                    
                    val fileName = inputPathObj.fileName?.toString() ?: ""
                    return if (fileName.isEmpty()) "" else "$fileName/"
                }
            }
        } catch (e: Exception) {
            LOG.error("Failed to process directory path", e)
            return ""
        }
    }
}