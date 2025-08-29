package com.jetbrains.rider.plugins.atomic.services

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiElement
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.jetbrains.rider.plugins.atomic.psi.*
import com.jetbrains.rider.plugins.atomic.psi.impl.AtomicPsiImplUtil
import com.jetbrains.rider.plugins.atomic.settings.AtomicPluginSettings
import kotlinx.coroutines.*
import java.io.File
import com.intellij.openapi.diagnostic.Logger

@Service(Service.Level.PROJECT)
class AtomicAutoGenerator(private val project: Project) {
    
    private val generationService = AtomicGenerationService.getInstance(project)
    private val fileTracker = GeneratedFileTracker.getInstance(project)
    private val fileValidator = AtomicFileValidator.getInstance(project)
    private val scope = CoroutineScope(Dispatchers.IO)
    private val processingFiles = mutableSetOf<String>()
    
    companion object {
        fun getInstance(project: Project): AtomicAutoGenerator {
            return project.getService(AtomicAutoGenerator::class.java)
        }
        
        private const val NOTIFICATION_GROUP = "Atomic Plugin"
        private val LOG = Logger.getInstance(AtomicAutoGenerator::class.java)
    }
    
    /**
     * Manual generation - allows creating new files (called from Ctrl+Shift+G)
     */
    fun generateManually(atomicFile: AtomicFile) {
        val fileName = atomicFile.name
        val filePath = atomicFile.virtualFile?.path ?: "unknown"
        LOG.info("AtomicAutoGenerator: generateManually called for $fileName at $filePath")
        
        // Call the internal generation method with forceCreate = true
        performGeneration(atomicFile, forceCreate = true)
    }
    
    /**
     * Auto-generation - only updates existing files
     */
    fun regenerateIfValid(atomicFile: AtomicFile) {
        val fileName = atomicFile.name
        val filePath = atomicFile.virtualFile?.path ?: "unknown"
        LOG.info("AtomicAutoGenerator: regenerateIfValid called for $fileName at $filePath")
        
        // Call the internal generation method with forceCreate = false
        performGeneration(atomicFile, forceCreate = false)
    }
    
    private fun performGeneration(atomicFile: AtomicFile, forceCreate: Boolean) {
        val fileName = atomicFile.name
        val filePath = atomicFile.virtualFile?.path ?: "unknown"
        LOG.info("AtomicAutoGenerator: performGeneration called for $fileName at $filePath, forceCreate=$forceCreate")
        
        val settings = AtomicPluginSettings.getInstance(project)
        if (!forceCreate && !settings.autoGenerateEnabled) {
            LOG.info("AtomicAutoGenerator: Auto-generation is disabled in settings")
            return
        }
        
        if (processingFiles.contains(filePath)) {
            LOG.info("AtomicAutoGenerator: Already processing $fileName, skipping")
            return
        }
        
        val commandProcessor = CommandProcessor.getInstance()
        if (commandProcessor.currentCommand != null) {
            LOG.info("AtomicAutoGenerator: Command in progress, scheduling for later")
            ApplicationManager.getApplication().invokeLater {
                performGeneration(atomicFile, forceCreate)
            }
            return
        }
        
        scope.launch {
            processingFiles.add(filePath)
            try {
                delay(100)
                
                val validationAndConfig = withContext(Dispatchers.Main) {
                    ReadAction.compute<Pair<ValidationResult, AtomicFileConfig?>, Exception> {
                        val validation = validateAtomicFile(atomicFile)
                        val config = if (validation.isValid) parseAtomicFileConfig(atomicFile) else null
                        validation to config
                    }
                }
                
                val validation = validationAndConfig.first
                val config = validationAndConfig.second
                
                if (!validation.isValid || config == null) {
                    LOG.info("AtomicAutoGenerator: File is not valid or config is null. Validation: ${validation.isValid}, Missing fields: ${validation.missingFields}")
                    return@launch
                }
                
                val hasErrors = withContext(Dispatchers.Main) {
                    ReadAction.compute<Boolean, Exception> {
                        fileValidator.hasErrors(atomicFile)
                    }
                }
                
                if (hasErrors) {
                    LOG.info("AtomicAutoGenerator: File has validation errors, skipping generation")
                    return@launch
                }
                
                if (!generationService.isReady()) {
                    LOG.warn("AtomicAutoGenerator: Generation service not ready, skipping")
                    return@launch
                }
                
                val atomicVirtualFile = withContext(Dispatchers.Main) {
                    ReadAction.compute<VirtualFile?, Exception> {
                        atomicFile.virtualFile
                    }
                } ?: return@launch
                
                val hasChanged = fileTracker.hasOutputChanged(
                    atomicVirtualFile,
                    config.directory,
                    config.className
                )
                
                val outputPath = calculateOutputPath(atomicVirtualFile, config)
                if (outputPath == null) {
                    val atomicFileName = withContext(Dispatchers.Main) {
                        ReadAction.compute<String, Exception> {
                            atomicFile.name
                        }
                    }
                    showNotification(
                        "Failed to calculate output path for $atomicFileName",
                        NotificationType.ERROR
                    )
                    return@launch
                }
                
                // For auto-generation, only update existing files, don't create new ones
                if (!forceCreate && !outputPath.exists()) {
                    LOG.info("AtomicAutoGenerator: Generated file does not exist at ${outputPath.absolutePath}, skipping auto-generation")
                    LOG.info("AtomicAutoGenerator: Use Ctrl+Shift+G to generate the file for the first time")
                    return@launch
                }
                
                LOG.info("AtomicAutoGenerator: Generating code for ${atomicFile.name}")
                val generatedCode = generationService.generateApi(atomicFile)
                if (generatedCode == null) {
                    LOG.error("AtomicAutoGenerator: Failed to generate code")
                    return@launch
                }
                LOG.info("AtomicAutoGenerator: Code generated successfully, length: ${generatedCode.length}")
                
                val atomicFileName = withContext(Dispatchers.Main) {
                    ReadAction.compute<String, Exception> {
                        atomicFile.name
                    }
                }
                
                val generatedFile = writeGeneratedFile(outputPath, generatedCode)
                if (generatedFile != null) {
                    fileTracker.trackGeneratedFile(
                        atomicVirtualFile,
                        generatedFile,
                        config.directory,
                        config.className
                    )
                    
                    if (hasChanged && settings.showNotifications) {
                        showNotification(
                            "Generated ${config.className}.cs from $atomicFileName",
                            NotificationType.INFORMATION
                        )
                    }
                }
                
            } catch (e: Exception) {
                LOG.error("AtomicAutoGenerator: Failed to auto-generate from $fileName", e)
            } finally {
                processingFiles.remove(filePath)
                LOG.debug("AtomicAutoGenerator: Finished processing $fileName")
            }
        }
    }
    
    /**
     * Validate that the atomic file has all required fields
     */
    private fun validateAtomicFile(atomicFile: AtomicFile): ValidationResult {
        val missingFields = mutableListOf<String>()
        val existingFields = mutableSetOf<String>()
        
        atomicFile.children.filterIsInstance<AtomicHeaderSection>().forEach { headerSection ->
            headerSection.children.forEach { child ->
                when (child) {
                    is AtomicHeaderProp -> {
                        existingFields.add("header")
                        if (!hasValue(child)) missingFields.add("header")
                    }
                    is AtomicEntityTypeProp -> {
                        existingFields.add("entityType")
                        if (!hasValue(child)) missingFields.add("entityType")
                    }
                    is AtomicAggressiveInliningProp -> {
                        existingFields.add("aggressiveInlining")
                    }
                    is AtomicUnsafeProp -> {
                        existingFields.add("unsafe")
                    }
                }
            }
        }
        
        fun checkElement(element: PsiElement) {
            element.children.forEach { child ->
                when (child) {
                    is AtomicNamespaceProp -> {
                        existingFields.add("namespace")
                        if (!hasValue(child)) missingFields.add("namespace")
                    }
                    is AtomicClassNameProp -> {
                        existingFields.add("className")
                        if (!hasValue(child)) missingFields.add("className")
                    }
                    is AtomicDirectoryProp -> {
                        existingFields.add("directory")
                        if (!hasValue(child)) missingFields.add("directory")
                    }
                    is AtomicSolutionProp -> {
                        existingFields.add("solution")
                        if (!hasValue(child)) missingFields.add("solution")
                    }
                    is AtomicAggressiveInliningProp -> {
                        existingFields.add("aggressiveInlining")
                    }
                    is AtomicUnsafeProp -> {
                        existingFields.add("unsafe")
                    }
                }
                checkElement(child)
            }
        }
        checkElement(atomicFile)
        
        val requiredFields = listOf("header", "entityType", "namespace", "className", "directory")
        requiredFields.forEach { field ->
            if (!existingFields.contains(field)) {
                missingFields.add(field)
            }
        }
        
        var hasTagsOrValues = false
        
        atomicFile.children.filterIsInstance<AtomicTagsSection>().forEach { tagsSection ->
            if (tagsSection.tagItemList.isNotEmpty()) {
                hasTagsOrValues = true
            }
        }
        
        if (!hasTagsOrValues) {
            atomicFile.children.filterIsInstance<AtomicValuesSection>().forEach { valuesSection ->
                if (valuesSection.valueItemList.isNotEmpty()) {
                    hasTagsOrValues = true
                }
            }
        }
        
        if (!hasTagsOrValues) {
            missingFields.add("tags or values")
        }
        
        return ValidationResult(
            isValid = missingFields.isEmpty() && hasTagsOrValues,
            missingFields = missingFields
        )
    }
    
    private fun hasValue(element: PsiElement): Boolean {
        val text = element.text
        val colonIndex = text.indexOf(':')
        if (colonIndex == -1) return false
        
        val value = text.substring(colonIndex + 1).trim()
        return value.isNotEmpty()
    }
    
    /**
     * Parse the atomic file to extract configuration
     */
    private fun parseAtomicFileConfig(atomicFile: AtomicFile): AtomicFileConfig? {
        return try {
            val config = AtomicFileConfig()
            
            atomicFile.children.filterIsInstance<AtomicHeaderSection>().forEach { headerSection ->
                headerSection.children.forEach { child ->
                    when (child) {
                        is AtomicHeaderProp -> config.header = AtomicPsiImplUtil.getValue(child) ?: ""
                        is AtomicEntityTypeProp -> config.entityType = AtomicPsiImplUtil.getValue(child) ?: ""
                        is AtomicAggressiveInliningProp -> config.aggressiveInlining = AtomicPsiImplUtil.getBooleanValue(child)
                        is AtomicUnsafeProp -> config.unsafe = AtomicPsiImplUtil.getBooleanValue(child)
                    }
                }
            }
            
            fun parseElement(element: PsiElement) {
                element.children.forEach { child ->
                    when (child) {
                        is AtomicNamespaceProp -> config.namespace = AtomicPsiImplUtil.getValue(child) ?: ""
                        is AtomicClassNameProp -> config.className = AtomicPsiImplUtil.getValue(child) ?: ""
                        is AtomicDirectoryProp -> config.directory = AtomicPsiImplUtil.getValue(child) ?: ""
                        is AtomicSolutionProp -> config.solution = AtomicPsiImplUtil.getValue(child) ?: ""
                        is AtomicAggressiveInliningProp -> config.aggressiveInlining = AtomicPsiImplUtil.getBooleanValue(child)
                        is AtomicUnsafeProp -> config.unsafe = AtomicPsiImplUtil.getBooleanValue(child)
                    }
                    parseElement(child)
                }
            }
            parseElement(atomicFile)
            
            config
        } catch (e: Exception) {
            null
        }
    }
    
    private fun getValue(element: PsiElement): String {
        val text = element.text
        val colonIndex = text.indexOf(':')
        if (colonIndex == -1) return ""
        
        return text.substring(colonIndex + 1).trim().removeSurrounding("\"")
    }
    
    private fun getBooleanValue(element: PsiElement): Boolean {
        return getValue(element).equals("true", ignoreCase = true)
    }
    
    /**
     * Calculate the output file path
     */
    private fun calculateOutputPath(atomicVirtualFile: VirtualFile, config: AtomicFileConfig): File? {
        val projectBasePath = project.basePath ?: return null
        val atomicFilePath = atomicVirtualFile.path
        
        val outputDir = if (File(config.directory).isAbsolute) {
            File(config.directory)
        } else {
            File(projectBasePath, config.directory)
        }
        
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        
        return File(outputDir, "${config.className}.cs")
    }
    
    /**
     * Write the generated code to file in a way that doesn't interfere with undo
     */
    private suspend fun writeGeneratedFile(outputFile: File, content: String): VirtualFile? {
        return withContext(Dispatchers.IO) {
            try {
                
                outputFile.parentFile?.mkdirs()
                outputFile.writeText(content)

                val virtualFile = VirtualFileManager.getInstance().refreshAndFindFileByNioPath(outputFile.toPath())
                virtualFile?.refresh(false, false)
                
                virtualFile
            } catch (e: Exception) {
                showNotification(
                    "Failed to write generated file: ${e.message}",
                    NotificationType.ERROR
                )
                null
            }
        }
    }
    
    private fun showNotification(content: String, type: NotificationType) {
        ApplicationManager.getApplication().invokeLater {
            Notifications.Bus.notify(
                Notification(NOTIFICATION_GROUP, "Atomic Plugin", content, type),
                project
            )
        }
    }
    
    fun dispose() {
        scope.cancel()
    }
    
    data class ValidationResult(
        val isValid: Boolean,
        val missingFields: List<String>
    )
    
    data class AtomicFileConfig(
        var header: String = "",
        var entityType: String = "",
        var namespace: String = "",
        var className: String = "",
        var directory: String = "",
        var solution: String = "",
        var aggressiveInlining: Boolean = false,
        var unsafe: Boolean = false
    )
}