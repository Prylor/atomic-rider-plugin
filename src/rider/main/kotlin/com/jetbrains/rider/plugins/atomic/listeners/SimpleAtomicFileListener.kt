package com.jetbrains.rider.plugins.atomic.listeners

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.jetbrains.rider.plugins.atomic.psi.AtomicFile
import com.jetbrains.rider.plugins.atomic.services.AtomicAutoGenerator
import com.jetbrains.rider.plugins.atomic.services.AtomicFileChangeAnalyzer
import com.jetbrains.rider.plugins.atomic.settings.AtomicPluginSettings
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Simple document-based listener for .atomic files
 */
class SimpleAtomicFileListener(private val project: Project) : Disposable {
    
    companion object {
        private val LOG = Logger.getInstance(SimpleAtomicFileListener::class.java)
    }
    
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val pendingJobs = ConcurrentHashMap<String, Job>()
    private val documentListeners = ConcurrentHashMap<Document, DocumentListener>()
    private val changeAnalyzer = AtomicFileChangeAnalyzer.getInstance(project)
    
    fun startListening() {
        LOG.info("SimpleAtomicFileListener: Starting to monitor .atomic files")
        
        
        val fileDocumentManager = FileDocumentManager.getInstance()
        
        ApplicationManager.getApplication().invokeLater {
            ReadAction.run<Exception> {
                
                val fileEditorManager = com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project)
                fileEditorManager.openFiles.forEach { file ->
                    if (file.extension == "atomic") {
                        val document = fileDocumentManager.getDocument(file)
                        if (document != null) {
                            attachDocumentListener(document, file)
                        }
                    }
                }
            }
        }
        
        project.messageBus.connect(this).subscribe(
            com.intellij.openapi.fileEditor.FileEditorManagerListener.FILE_EDITOR_MANAGER,
            object : com.intellij.openapi.fileEditor.FileEditorManagerListener {
                override fun fileOpened(source: com.intellij.openapi.fileEditor.FileEditorManager, file: VirtualFile) {
                    if (file.extension == "atomic") {
                        val document = fileDocumentManager.getDocument(file)
                        if (document != null) {
                            attachDocumentListener(document, file)
                        }
                    }
                }
            }
        )
    }
    
    private fun attachDocumentListener(document: Document, file: VirtualFile) {
        if (documentListeners.containsKey(document)) {
            return
        }
        
        LOG.info("SimpleAtomicFileListener: Attaching listener to ${file.path}")
        
        val listener = object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                LOG.debug("SimpleAtomicFileListener: Document changed for ${file.path}")
                
                
                val currentContent = document.text
                val previousContent = changeAnalyzer.getPreviousContent(file.path)
                
                ApplicationManager.getApplication().invokeLater {
                    ReadAction.run<Exception> {
                        val psiFile = PsiManager.getInstance(project).findFile(file) as? AtomicFile
                        if (psiFile != null) {
                            if (changeAnalyzer.shouldRegenerate(psiFile, previousContent, currentContent)) {
                                LOG.info("SimpleAtomicFileListener: Regeneration needed for ${file.path}")
                                
                                if (CommandProcessor.getInstance().currentCommand == null) {
                                    scheduleRegeneration(file)
                                } else {
                                    
                                    ApplicationManager.getApplication().invokeLater {
                                        scheduleRegeneration(file)
                                    }
                                }
                            } else {
                                LOG.debug("SimpleAtomicFileListener: No regeneration needed for ${file.path}")
                            }
                            
                            
                            changeAnalyzer.updateContent(file.path, currentContent)
                        }
                    }
                }
            }
        }
        
        document.addDocumentListener(listener, this)
        documentListeners[document] = listener
        
        changeAnalyzer.updateContent(file.path, document.text)
    }
    
    private fun scheduleRegeneration(file: VirtualFile) {
        val settings = AtomicPluginSettings.getInstance(project)
        if (!settings.autoGenerateEnabled) {
            LOG.info("SimpleAtomicFileListener: Auto-generation disabled")
            return
        }
        
        val path = file.path
        
        pendingJobs[path]?.cancel()
        
        val job = scope.launch {
            delay(settings.debounceDelayMs)
            
            LOG.info("SimpleAtomicFileListener: Triggering regeneration for $path")
            
            
            ApplicationManager.getApplication().invokeLater {
                
                if (CommandProcessor.getInstance().currentCommand != null) {
                    
                    ApplicationManager.getApplication().invokeLater {
                        performRegeneration(file)
                    }
                } else {
                    performRegeneration(file)
                }
            }
            
            pendingJobs.remove(path)
        }
        
        pendingJobs[path] = job
    }
    
    private fun performRegeneration(file: VirtualFile) {
        ReadAction.run<Exception> {
            val psiFile = PsiManager.getInstance(project).findFile(file) as? AtomicFile
            if (psiFile != null) {
                LOG.info("SimpleAtomicFileListener: Found PSI file, calling auto generator")
                AtomicAutoGenerator.getInstance(project).regenerateIfValid(psiFile)
            } else {
                LOG.warn("SimpleAtomicFileListener: Could not find PSI file for ${file.path}")
            }
        }
    }
    
    override fun dispose() {
        LOG.info("SimpleAtomicFileListener: Disposing")
        scope.cancel()
        pendingJobs.clear()
        documentListeners.clear()
    }
}