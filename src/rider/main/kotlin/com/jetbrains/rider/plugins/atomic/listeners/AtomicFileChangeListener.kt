package com.jetbrains.rider.plugins.atomic.listeners

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectLocator
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.*
import com.intellij.psi.PsiManager
import com.jetbrains.rider.plugins.atomic.psi.AtomicFile
import com.jetbrains.rider.plugins.atomic.services.AtomicGenerationService
import com.jetbrains.rider.plugins.atomic.services.GeneratedFileTracker
import com.jetbrains.rider.plugins.atomic.services.AtomicAutoGenerator
import com.jetbrains.rider.plugins.atomic.services.AtomicFileChangeAnalyzer
import com.jetbrains.rider.plugins.atomic.settings.AtomicPluginSettings
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import com.intellij.openapi.diagnostic.Logger

class AtomicFileChangeListener : BulkFileListener {
    private val pendingRegenerations = ConcurrentHashMap<String, Job>()
    private val regenerationScope = CoroutineScope(Dispatchers.IO)
    
    companion object {
        private const val DEFAULT_DEBOUNCE_DELAY_MS = 500L
        private val LOG = Logger.getInstance(AtomicFileChangeListener::class.java)
    }
    
    override fun after(events: List<VFileEvent>) {
        LOG.info("AtomicFileChangeListener: Received ${events.size} file events")
        for (event in events) {
            LOG.debug("AtomicFileChangeListener: Event type: ${event.javaClass.simpleName} for file: ${event.file?.path}")
            when (event) {
                is VFileContentChangeEvent -> handleContentChange(event)
                is VFileCreateEvent -> handleFileCreated(event)
                is VFileDeleteEvent -> handleFileDeleted(event)
                is VFileMoveEvent -> handleFileMoved(event)
                is VFileCopyEvent -> handleFileCopied(event)
                is VFilePropertyChangeEvent -> handlePropertyChange(event)
            }
        }
    }
    
    private fun handleContentChange(event: VFileContentChangeEvent) {
        val file = event.file
        LOG.debug("AtomicFileChangeListener: Content changed for file: ${file?.path}")
        if (!isAtomicFile(file)) {
            LOG.debug("AtomicFileChangeListener: Not an atomic file, skipping")
            return
        }
        LOG.info("AtomicFileChangeListener: Atomic file changed: ${file.path}")
        
        
        val project = findProject(file)
        if (project != null) {
            ApplicationManager.getApplication().invokeLater {
                ReadAction.run<Exception> {
                    val psiFile = PsiManager.getInstance(project).findFile(file) as? AtomicFile
                    if (psiFile != null) {
                        val analyzer = AtomicFileChangeAnalyzer.getInstance(project)
                        val currentContent = psiFile.text
                        val previousContent = analyzer.getPreviousContent(file.path)
                        
                        if (analyzer.shouldRegenerate(psiFile, previousContent, currentContent)) {
                            LOG.info("AtomicFileChangeListener: Regeneration needed for ${file.path}")
                            scheduleRegeneration(file)
                        } else {
                            LOG.debug("AtomicFileChangeListener: No regeneration needed for ${file.path}")
                        }
                        
                        
                        analyzer.updateContent(file.path, currentContent)
                    }
                }
            }
        } else {
            
            scheduleRegeneration(file)
        }
    }
    
    private fun handleFileCreated(event: VFileCreateEvent) {
        
        
    }
    
    private fun handleFileDeleted(event: VFileDeleteEvent) {
        val file = event.file
        if (!isAtomicFile(file)) return
        
        
        findProject(file)?.let { project ->
            val tracker = GeneratedFileTracker.getInstance(project)
            tracker.removeTracking(file)
        }
    }
    
    private fun handleFileMoved(event: VFileMoveEvent) {
        val file = event.file
        if (!isAtomicFile(file)) return
        
        
        scheduleRegeneration(file)
    }
    
    private fun handleFileCopied(event: VFileCopyEvent) {
        
    }
    
    private fun handlePropertyChange(event: VFilePropertyChangeEvent) {
        val file = event.file
        if (!isAtomicFile(file)) return
        
        
        if (event.propertyName == VirtualFile.PROP_NAME) {
            val oldName = event.oldValue as? String
            val newName = event.newValue as? String
            
            if (oldName != null && newName != null && oldName.endsWith(".atomic") && newName.endsWith(".atomic")) {
                
                scheduleRegeneration(file)
            }
        }
    }
    
    private fun isAtomicFile(file: VirtualFile?): Boolean {
        return file != null && file.extension == "atomic" && !file.isDirectory
    }
    
    private fun scheduleRegeneration(file: VirtualFile) {
        val filePath = file.path
        LOG.info("AtomicFileChangeListener: Scheduling regeneration for: $filePath")
        
        
        pendingRegenerations[filePath]?.cancel()
        
        
        val project = findProject(file)
        if (project == null) {
            LOG.warn("AtomicFileChangeListener: Could not find project for file $filePath")
            return
        }
        
        val settings = AtomicPluginSettings.getInstance(project)
        val debounceDelay = settings.debounceDelayMs
        
        
        val job = regenerationScope.launch {
            LOG.debug("AtomicFileChangeListener: Waiting $debounceDelay ms before regenerating $filePath")
            delay(debounceDelay)
            
            LOG.info("AtomicFileChangeListener: Starting regeneration for $filePath")
            regenerateFile(project, file)
            pendingRegenerations.remove(filePath)
        }
        
        pendingRegenerations[filePath] = job
    }
    
    private suspend fun regenerateFile(project: Project, file: VirtualFile) {
        LOG.debug("AtomicFileChangeListener: regenerateFile called for ${file.path}")
        val psiFile = withContext(Dispatchers.Main) {
            ReadAction.compute<AtomicFile?, Exception> {
                PsiManager.getInstance(project).findFile(file) as? AtomicFile
            }
        }
        
        if (psiFile != null) {
            LOG.info("AtomicFileChangeListener: Found PSI file, calling auto generator")
            val autoGenerator = AtomicAutoGenerator.getInstance(project)
            autoGenerator.regenerateIfValid(psiFile)
        } else {
            LOG.warn("AtomicFileChangeListener: Could not find PSI file for ${file.path}")
        }
    }
    
    private fun findProject(file: VirtualFile): Project? {
        return ProjectLocator.getInstance().guessProjectForFile(file)
    }
    
    fun dispose() {
        
        pendingRegenerations.values.forEach { it.cancel() }
        pendingRegenerations.clear()
        regenerationScope.cancel()
    }
}