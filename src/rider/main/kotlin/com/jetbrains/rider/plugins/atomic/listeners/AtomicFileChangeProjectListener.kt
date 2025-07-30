package com.jetbrains.rider.plugins.atomic.listeners

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.AsyncFileListener
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.events.*
import com.intellij.psi.PsiManager
import com.jetbrains.rider.plugins.atomic.psi.AtomicFile
import com.jetbrains.rider.plugins.atomic.services.AtomicAutoGenerator
import com.jetbrains.rider.plugins.atomic.settings.AtomicPluginSettings
import kotlinx.coroutines.*

@Service(Service.Level.PROJECT)
class AtomicFileChangeProjectListener(private val project: Project) : AsyncFileListener {
    
    companion object {
        private val LOG = Logger.getInstance(AtomicFileChangeProjectListener::class.java)
        
        fun getInstance(project: Project): AtomicFileChangeProjectListener {
            return project.getService(AtomicFileChangeProjectListener::class.java)
        }
    }
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val pendingChanges = mutableMapOf<String, Job>()
    
    override fun prepareChange(events: List<VFileEvent>): AsyncFileListener.ChangeApplier? {
        val atomicEvents = events.filter { event ->
            event.file?.extension == "atomic" && event is VFileContentChangeEvent
        }
        
        if (atomicEvents.isEmpty()) {
            return null
        }
        
        LOG.info("AtomicFileChangeProjectListener: Found ${atomicEvents.size} atomic file change events")
        
        return object : AsyncFileListener.ChangeApplier {
            override fun afterVfsChange() {
                val settings = AtomicPluginSettings.getInstance(project)
                if (!settings.autoGenerateEnabled) {
                    LOG.info("AtomicFileChangeProjectListener: Auto-generation disabled")
                    return
                }
                
                atomicEvents.forEach { event ->
                    val file = event.file ?: return@forEach
                    val path = file.path
                    
                    LOG.info("AtomicFileChangeProjectListener: Scheduling regeneration for $path")
                    
                    
                    pendingChanges[path]?.cancel()
                    
                    
                    val job = scope.launch {
                        delay(settings.debounceDelayMs)
                        
                        LOG.info("AtomicFileChangeProjectListener: Triggering regeneration for $path")
                        
                        withContext(Dispatchers.Main) {
                            com.intellij.openapi.application.ReadAction.run<Exception> {
                                val psiFile = PsiManager.getInstance(project).findFile(file) as? AtomicFile
                                if (psiFile != null) {
                                    AtomicAutoGenerator.getInstance(project).regenerateIfValid(psiFile)
                                } else {
                                    LOG.warn("AtomicFileChangeProjectListener: Could not find PSI file for $path")
                                }
                            }
                        }
                        
                        pendingChanges.remove(path)
                    }
                    
                    pendingChanges[path] = job
                }
            }
        }
    }
    
    fun dispose() {
        scope.cancel()
        pendingChanges.clear()
    }
}