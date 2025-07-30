package com.jetbrains.rider.plugins.atomic.startup

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.util.messages.MessageBusConnection
import com.jetbrains.rider.plugins.atomic.services.GeneratedFileTracker

class AtomicFileWatcher : StartupActivity.DumbAware {
    
    override fun runActivity(project: Project) {
        ApplicationManager.getApplication().executeOnPooledThread {
            val tracker = GeneratedFileTracker.getInstance(project)
            tracker.cleanupOrphanedFiles()
        }
        
        project.basePath?.let { basePath ->
            val projectDir = VirtualFileManager.getInstance().findFileByUrl("file://$basePath")
            projectDir?.refresh(true, true)
        }
    }
}