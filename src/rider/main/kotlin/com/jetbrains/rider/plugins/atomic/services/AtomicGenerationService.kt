package com.jetbrains.rider.plugins.atomic.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.jetbrains.rdclient.util.idea.LifetimedProjectComponent
import com.jetbrains.rider.projectView.solution
import com.jetbrains.rider.plugins.atomic.model.*
import com.jetbrains.rider.plugins.atomic.psi.*
import com.jetbrains.rider.plugins.atomic.psi.impl.AtomicPsiImplUtil
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.progress.ProcessCanceledException
import com.jetbrains.rider.protocol.protocol
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.jetbrains.rd.util.reactive.adviseEternal
import com.jetbrains.rd.util.threading.coroutines.launch
import kotlinx.coroutines.*
import com.jetbrains.rd.util.reactive.valueOrDefault

@Service(Service.Level.PROJECT)
class AtomicGenerationService(project: Project) : LifetimedProjectComponent(project) {
    companion object {
        private val logger = Logger.getInstance(AtomicGenerationService::class.java)
        
        fun getInstance(project: Project): AtomicGenerationService {
            return project.getService(AtomicGenerationService::class.java)
        }
    }
    
    private val model = project.solution.atomicGenerationModel
    private var isBackendReady = false
    private val readinessCallbacks = mutableListOf<() -> Unit>()
    private val pendingFiles = mutableSetOf<PsiFile>()
    
    init {
        project.protocol.scheduler.invokeOrQueue {
            model.generationStatus.advise(componentLifetime) { status ->
                logger.info("Generation status: $status")
            }
            
            project.solution.isLoaded.advise(componentLifetime) { isLoaded ->
                if (isLoaded) {
                    GlobalScope.launch {
                        delay(1000)
                        checkBackendReadiness()
                    }
                }
            }
        }
    }
    
    private suspend fun checkBackendReadiness() {
        try {
            val testResponse = validateType("string", emptyList(), project.basePath ?: "")
            if (testResponse != null) {
                isBackendReady = true
                logger.info("Backend is ready")
                
                withContext(Dispatchers.Main) {
                    readinessCallbacks.forEach { it.invoke() }
                    readinessCallbacks.clear()
                    
                    
                    pendingFiles.forEach { file ->
                        if (file.isValid) {
                            DaemonCodeAnalyzer.getInstance(project).restart(file)
                        }
                    }
                    pendingFiles.clear()
                }
            }
        } catch (e: Exception) {
            logger.warn("Backend not ready yet: ${e.message}")
            
            GlobalScope.launch {
                delay(2000)
                checkBackendReadiness()
            }
        }
    }
    
    fun isReady(): Boolean = isBackendReady
    
    fun whenReady(callback: () -> Unit) {
        if (isBackendReady) {
            callback.invoke()
        } else {
            readinessCallbacks.add(callback)
        }
    }
    
    fun registerFileForReAnnotation(file: PsiFile) {
        if (!isBackendReady && file !in pendingFiles) {
            pendingFiles.add(file)
        }
    }

    suspend fun generateApi(atomicFile: AtomicFile): String? {
        return try {
            val fileData = parseAtomicFile(atomicFile)
            
            logger.info("Sending generation request for: ${fileData.filePath}")
            logger.info("Parsed file data - Headers: ${fileData.headerProperties.size}, Values: ${fileData.values.size}")
            
            val generatedCode = model.generateApi.startSuspending(componentLifetime, fileData)
            
            logger.info("Generation completed successfully")
            generatedCode
        } catch (e: Exception) {
            logger.error("Failed to generate API: ${e.message}", e)
            logger.error("Stack trace:", e)
            throw e  
        }
    }
    
    suspend fun getTypeCompletions(prefix: String, imports: List<String>, projectPath: String, namespaceFilter: String? = null): List<TypeCompletionItem> {
        return try {
            val request = TypeCompletionRequest(
                prefix = prefix,
                imports = imports.toTypedArray(),
                projectPath = projectPath,
                namespaceFilter = namespaceFilter
            )
            
            val response = model.getTypeCompletions.startSuspending(componentLifetime, request)
            response.items.toList()
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            emptyList()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.error("Failed to get type completions", e)
            emptyList()
        }
    }
    
    suspend fun validateType(typeName: String, imports: List<String>, projectPath: String): TypeValidationResponse? {
        return try {
            val request = TypeValidationRequest(
                typeName = typeName,
                imports = imports.toTypedArray(),
                projectPath = projectPath
            )
            
            model.validateType.startSuspending(componentLifetime, request)
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            null
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.error("Failed to validate type", e)
            null
        }
    }

    suspend fun getAvailableProjects(): List<String> {
        return try {
            model.getAvailableProjects.startSuspending(componentLifetime, Unit).toList()
        } catch (e: Exception) {
            logger.error("Failed to get available projects", e)
            emptyList()
        }
    }
    
    suspend fun getNamespaceCompletions(prefix: String, projectPath: String): List<String> {
        return try {
            val request = NamespaceCompletionRequest(
                prefix = prefix,
                projectPath = projectPath
            )
            
            val response = model.getNamespaceCompletions.startSuspending(componentLifetime, request)
            response.namespaces.toList()
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            emptyList()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.error("Failed to get namespace completions", e)
            emptyList()
        }
    }
    
    suspend fun validateNamespace(namespace: String, projectPath: String): NamespaceValidationResponse? {
        return try {
            val request = NamespaceValidationRequest(
                namespace = namespace,
                projectPath = projectPath
            )
            
            model.validateNamespace.startSuspending(componentLifetime, request)
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            null
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.error("Failed to validate namespace", e)
            null
        }
    }
    
    fun calculateGeneratedFilePath(atomicFile: AtomicFile): String? {
        return try {
            val fileData = parseAtomicFile(atomicFile)
            val atomicPath = atomicFile.virtualFile.path
            
            
            var className = "AtomicAPIExtensions"
            var directory: String? = null
            
            fileData.headerProperties.forEach { prop ->
                when (prop.key) {
                    "className" -> className = prop.value
                    "directory" -> directory = prop.value
                }
            }
            
            logger.info("[calculateGeneratedFilePath] atomicPath: $atomicPath")
            logger.info("[calculateGeneratedFilePath] directory: '$directory'")
            logger.info("[calculateGeneratedFilePath] className: $className")
            
            val outputFileName = "$className.cs"
            
            
            when {
                !directory.isNullOrEmpty() -> {
                    val cleanDir = directory!!.replace("./", "").replace(".\\", "").trim('/', '\\')
                    
                    logger.info("[calculateGeneratedFilePath] cleanDir: '$cleanDir'")
                    
                    
                    if (java.io.File(directory).isAbsolute) {
                        logger.info("[calculateGeneratedFilePath] Using absolute path")
                        java.nio.file.Paths.get(directory, outputFileName).toString()
                    } else if (cleanDir.isEmpty() || cleanDir == ".") {
                        
                        logger.info("[calculateGeneratedFilePath] Directory is current dir, using atomic file dir")
                        val atomicFileDir = java.io.File(atomicPath).parent
                        java.nio.file.Paths.get(atomicFileDir, outputFileName).toString()
                    } else if (cleanDir.startsWith("Assets", ignoreCase = true)) {
                        
                        logger.info("[calculateGeneratedFilePath] Directory starts with Assets, finding Unity root")
                        val atomicFileDir = java.io.File(atomicPath).parent
                        
                        
                        if (atomicFileDir.endsWith(cleanDir.replace('\\', java.io.File.separatorChar).replace('/', java.io.File.separatorChar))) {
                            logger.info("[calculateGeneratedFilePath] Atomic file is already in the target directory")
                            return java.nio.file.Paths.get(atomicFileDir, outputFileName).toString()
                        }
                        
                        
                        var currentDir = java.io.File(atomicFileDir)
                        while (currentDir != null) {
                            val assetsDir = java.io.File(currentDir, "Assets")
                            if (assetsDir.exists() && assetsDir.isDirectory) {
                                
                                logger.info("[calculateGeneratedFilePath] Found Unity root at: ${currentDir.absolutePath}")
                                return java.nio.file.Paths.get(currentDir.absolutePath, cleanDir, outputFileName).toString()
                            }
                            currentDir = currentDir.parentFile
                        }
                        
                        
                        logger.warn("[calculateGeneratedFilePath] Unity root not found, using relative path")
                        java.nio.file.Paths.get(atomicFileDir, cleanDir, outputFileName).toString()
                    } else {
                        
                        logger.info("[calculateGeneratedFilePath] Using relative path")
                        val atomicFileDir = java.io.File(atomicPath).parent
                        java.nio.file.Paths.get(atomicFileDir, cleanDir, outputFileName).toString()
                    }
                }
                else -> {
                    
                    val atomicFileDir = java.io.File(atomicPath).parent
                    java.nio.file.Paths.get(atomicFileDir, outputFileName).toString()
                }
            }
        } catch (e: ProcessCanceledException) {
            
            throw e
        } catch (e: Exception) {
            logger.error("Failed to calculate generated file path: ${e.message}")
            null
        }
    }
    
    suspend fun findGeneratedMethodUsages(valueName: String, methodNames: List<String>, projectPath: String, generatedFilePath: String): List<MethodUsageLocation> {
        return try {
            val request = FindMethodUsagesRequest(
                valueName = valueName,
                methodNames = methodNames.toTypedArray(),
                projectPath = projectPath,
                generatedFilePath = generatedFilePath
            )
            
            val response = model.findMethodUsages.startSuspending(componentLifetime, request)
            response.usages.toList()
        } catch (e: Exception) {
            logger.error("Failed to find method usages", e)
            emptyList()
        }
    }

    suspend fun findGeneratedTagUsages(tagName: String, methodNames: List<String>, projectPath: String, generatedFilePath: String): List<MethodUsageLocation> {
        return try {
            val request = FindTagUsagesRequest(
                tagName = tagName,
                methodNames = methodNames.toTypedArray(),
                projectPath = projectPath,
                generatedFilePath = generatedFilePath
            )
            
            val response = model.findTagUsages.startSuspending(componentLifetime, request)
            response.usages.toList()
        } catch (e: Exception) {
            logger.error("Failed to find tag usages", e)
            emptyList()
        }
    }
    

    suspend fun renameValue(atomicFilePath: String, oldName: String, newName: String, projectPath: String): RenameResponse {
        return try {
            val request = RenameValueRequest(
                atomicFilePath = atomicFilePath,
                oldName = oldName,
                newName = newName,
                projectPath = projectPath
            )
            
            
            withContext(Dispatchers.IO) {
                withTimeout(10000L) {
                    model.renameValue.startSuspending(componentLifetime, request)
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to rename value", e)
            
            if (e is kotlinx.coroutines.TimeoutCancellationException) {
                logger.warn("Rename timed out, trying async approach")
                try {
                    val request2 = RenameValueRequest(
                        atomicFilePath = atomicFilePath,
                        oldName = oldName,
                        newName = newName,
                        projectPath = projectPath
                    )
                    
                    model.renameValue.start(componentLifetime, request2)
                    
                    RenameResponse(
                        success = true,
                        regeneratedFilePath = null,
                        updatedUsages = emptyArray(),
                        errorMessage = null
                    )
                } catch (e2: Exception) {
                    logger.error("Failed to rename value even with async", e2)
                    RenameResponse(
                        success = false,
                        regeneratedFilePath = null,
                        updatedUsages = emptyArray(),
                        errorMessage = e2.message
                    )
                }
            } else {
                RenameResponse(
                    success = false,
                    regeneratedFilePath = null,
                    updatedUsages = emptyArray(),
                    errorMessage = e.message
                )
            }
        }
    }

    suspend fun renameTag(atomicFilePath: String, oldName: String, newName: String, projectPath: String): RenameResponse {
        return try {
            val request = RenameTagRequest(
                atomicFilePath = atomicFilePath,
                oldName = oldName,
                newName = newName,
                projectPath = projectPath
            )
            
            
            withContext(Dispatchers.IO) {
                withTimeout(10000L) {
                    model.renameTag.startSuspending(componentLifetime, request)
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to rename tag", e)
            
            if (e is kotlinx.coroutines.TimeoutCancellationException) {
                logger.warn("Rename timed out, trying async approach")
                try {
                    val request2 = RenameTagRequest(
                        atomicFilePath = atomicFilePath,
                        oldName = oldName,
                        newName = newName,
                        projectPath = projectPath
                    )
                    
                    model.renameTag.start(componentLifetime, request2)
                    
                    RenameResponse(
                        success = true,
                        regeneratedFilePath = null,
                        updatedUsages = emptyArray(),
                        errorMessage = null
                    )
                } catch (e2: Exception) {
                    logger.error("Failed to rename tag even with async", e2)
                    RenameResponse(
                        success = false,
                        regeneratedFilePath = null,
                        updatedUsages = emptyArray(),
                        errorMessage = e2.message
                    )
                }
            } else {
                RenameResponse(
                    success = false,
                    regeneratedFilePath = null,
                    updatedUsages = emptyArray(),
                    errorMessage = e.message
                )
            }
        }
    }
    
    private fun parseAtomicFile(atomicFile: AtomicFile): AtomicFileData {
        val filePath = atomicFile.virtualFile.path
        
        return ReadAction.compute<AtomicFileData, Exception> {
            logger.info("Parsing atomic file, text preview: ${atomicFile.text.take(200)}...")
            val headerProperties = mutableListOf<HeaderProperty>()
            val imports = mutableListOf<String>()
            val tags = mutableListOf<String>()
            val values = mutableListOf<AtomicValueData>()
        
        
        atomicFile.children.filterIsInstance<AtomicHeaderSection>().forEach { headerSection ->
            logger.info("Header section found, children count: ${headerSection.children.size}")
            headerSection.children.forEach { child ->
                logger.info("Header child: ${child.javaClass.simpleName} - ${child.text}")
            }
            
            headerSection.headerPropList.forEach { prop ->
                val key = prop.firstChild?.text ?: return@forEach
                val value = prop.lastChild?.text?.trim('"') ?: return@forEach
                logger.info("Header property from headerPropList: key='$key', value='$value'")
                headerProperties.add(HeaderProperty(key, value))
            }
            
            
            headerSection.entityTypePropList.firstOrNull()?.let { prop ->
                val value = AtomicPsiImplUtil.getValue(prop)
                if (value != null) headerProperties.add(HeaderProperty("entityType", value))
            }
            headerSection.aggressiveInliningPropList.firstOrNull()?.let { prop ->
                val value = AtomicPsiImplUtil.getBooleanValue(prop)
                headerProperties.add(HeaderProperty("aggressiveInlining", value.toString()))
            }
            headerSection.unsafePropList.firstOrNull()?.let { prop ->
                val value = AtomicPsiImplUtil.getBooleanValue(prop)
                headerProperties.add(HeaderProperty("unsafe", value.toString()))
            }
            headerSection.namespacePropList.firstOrNull()?.let { prop ->
                val value = AtomicPsiImplUtil.getValue(prop)
                if (value != null) headerProperties.add(HeaderProperty("namespace", value))
            }
            headerSection.classNamePropList.firstOrNull()?.let { prop ->
                val value = AtomicPsiImplUtil.getValue(prop)
                if (value != null) headerProperties.add(HeaderProperty("className", value))
            }
            logger.info("directoryPropList size: ${headerSection.directoryPropList.size}")
            headerSection.directoryPropList.firstOrNull()?.let { prop ->
                logger.info("DirectoryProp found, node text: '${prop.node.text}'")
                val value = AtomicPsiImplUtil.getValue(prop)
                logger.info("Directory value: '$value'")
                if (value != null) headerProperties.add(HeaderProperty("directory", value))
            }
            headerSection.solutionPropList.firstOrNull()?.let { prop ->
                val value = AtomicPsiImplUtil.getValue(prop)
                if (value != null) headerProperties.add(HeaderProperty("solution", value))
            }
        }
        
        
        atomicFile.children.forEach { child ->
            when (child) {
                is AtomicNamespaceProp -> {
                    if (!headerProperties.any { it.key == "namespace" }) {
                        val value = AtomicPsiImplUtil.getValue(child)
                        if (value != null) headerProperties.add(HeaderProperty("namespace", value))
                    }
                }
                is AtomicAggressiveInliningProp -> {
                    if (!headerProperties.any { it.key == "aggressiveInlining" }) {
                        val value = AtomicPsiImplUtil.getBooleanValue(child)
                        headerProperties.add(HeaderProperty("aggressiveInlining", value.toString()))
                    }
                }
                is AtomicUnsafeProp -> {
                    if (!headerProperties.any { it.key == "unsafe" }) {
                        val value = AtomicPsiImplUtil.getBooleanValue(child)
                        headerProperties.add(HeaderProperty("unsafe", value.toString()))
                    }
                }
                is AtomicClassNameProp -> {
                    if (!headerProperties.any { it.key == "className" }) {
                        val value = AtomicPsiImplUtil.getValue(child)
                        if (value != null) headerProperties.add(HeaderProperty("className", value))
                    }
                }
                is AtomicDirectoryProp -> {
                    if (!headerProperties.any { it.key == "directory" }) {
                        val value = AtomicPsiImplUtil.getValue(child)
                        if (value != null) headerProperties.add(HeaderProperty("directory", value))
                    }
                }
                is AtomicSolutionProp -> {
                    if (!headerProperties.any { it.key == "solution" }) {
                        val value = AtomicPsiImplUtil.getValue(child)
                        if (value != null) headerProperties.add(HeaderProperty("solution", value))
                    }
                }
                is AtomicEntityTypeProp -> {
                    if (!headerProperties.any { it.key == "entityType" }) {
                        val value = AtomicPsiImplUtil.getValue(child)
                        if (value != null) headerProperties.add(HeaderProperty("entityType", value))
                    }
                }
                is AtomicHeaderProp -> {
                    if (!headerProperties.any { it.key == "header" }) {
                        val value = AtomicPsiImplUtil.getValue(child)
                        if (value != null) headerProperties.add(HeaderProperty("header", value))
                    }
                }
            }
        }
        
        
        atomicFile.children.filterIsInstance<AtomicImportsSection>().forEach { importsSection ->
            importsSection.importItemList.forEach { import ->
                val importPath = import.node.findChildByType(AtomicTypes.IMPORT_PATH)?.text
                if (importPath != null) {
                    imports.add(importPath)
                }
            }
        }
        
        
        atomicFile.children.filterIsInstance<AtomicTagsSection>().forEach { tagsSection ->
            tagsSection.tagItemList.forEach { tag ->
                val tagName = tag.node.findChildByType(AtomicTypes.TAG_NAME)?.text
                if (tagName != null) {
                    tags.add(tagName)
                }
            }
        }
        
        
        atomicFile.children.filterIsInstance<AtomicValuesSection>().forEach { valuesSection ->
            valuesSection.valueItemList.forEach { value ->
                val name = value.node.findChildByType(AtomicTypes.VALUE_NAME)?.text
                val type = AtomicPsiImplUtil.getTypeReference(value)
                if (name != null && type != null) {
                    logger.info("Parsed value: name='$name', type='$type'")
                    values.add(AtomicValueData(name, type))
                }
            }
        }
        
            logger.info("Parsed header properties: ${headerProperties.map { "${it.key}='${it.value}'" }.joinToString(", ")}")
            logger.info("Parsed imports: ${imports.joinToString(", ")}")
            logger.info("Parsed tags: ${tags.joinToString(", ")}")
            logger.info("Parsed values: ${values.map { "${it.name}: ${it.type}" }.joinToString(", ")}")
            AtomicFileData(
                headerProperties = headerProperties.toTypedArray(),
                imports = imports.toTypedArray(),
                tags = tags.toTypedArray(),
                values = values.toTypedArray(),
                filePath = filePath
            )
        }
    }
    
    suspend fun addAtomicFileToProject(atomicFilePath: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                model.addAtomicFileToProject.startSuspending(componentLifetime, atomicFilePath)
            } catch (e: Exception) {
                logger.error("Failed to add atomic file to project", e)
                false
            }
        }
    }
}