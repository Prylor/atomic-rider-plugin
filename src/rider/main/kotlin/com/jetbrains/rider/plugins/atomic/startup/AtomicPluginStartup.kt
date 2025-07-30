package com.jetbrains.rider.plugins.atomic.startup

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.vfs.VirtualFileManager
import com.jetbrains.rider.plugins.atomic.listeners.SimpleAtomicFileListener
import com.jetbrains.rider.plugins.atomic.services.AtomicGenerationService
import com.jetbrains.rider.plugins.atomic.services.GeneratedFileTracker
import com.intellij.openapi.util.Disposer

class AtomicPluginStartup : StartupActivity.DumbAware {
    
    companion object {
        private val LOG = Logger.getInstance(AtomicPluginStartup::class.java)
    }
    
    override fun runActivity(project: Project) {
        LOG.info("AtomicPlugin: Starting up for project ${project.name}")
        
        // Initialize services
        val generationService = AtomicGenerationService.getInstance(project)
        LOG.info("AtomicPlugin: Generation service initialized")
        
        // Clean up any orphaned generated files on startup
        ApplicationManager.getApplication().executeOnPooledThread {
            val tracker = GeneratedFileTracker.getInstance(project)
            tracker.cleanupOrphanedFiles()
            LOG.info("AtomicPlugin: Cleaned up orphaned files")
        }
        
        // Force refresh of project files to catch external changes
        project.basePath?.let { basePath ->
            val projectDir = VirtualFileManager.getInstance().findFileByUrl("file://$basePath")
            projectDir?.refresh(true, true)
            LOG.info("AtomicPlugin: Refreshed project files at $basePath")
        }
        
        // Log that we're ready
        LOG.info("AtomicPlugin: Startup complete. File change listener should be active.")
        
        // Start simple document-based listener as a fallback
        try {
            val simpleListener = SimpleAtomicFileListener(project)
            Disposer.register(project, simpleListener)
            simpleListener.startListening()
            LOG.info("AtomicPlugin: Started simple document-based file listener")
        } catch (e: Exception) {
            LOG.error("AtomicPlugin: Failed to start simple listener", e)
        }
        
        // Note: File change listener is also registered as an applicationListener in plugin.xml
        LOG.info("AtomicPlugin: Multiple file change detection mechanisms are active")
    }
}