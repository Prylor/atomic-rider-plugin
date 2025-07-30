package com.jetbrains.rider.plugins.atomic.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.jetbrains.rider.plugins.atomic.settings.AtomicPluginSettings
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.PROJECT)
@State(
    name = "AtomicGeneratedFileTracker",
    storages = [Storage("atomicGeneratedFiles.xml")]
)
class GeneratedFileTracker(private val project: Project) : PersistentStateComponent<GeneratedFileTracker.State> {
    
    data class State(
        var trackedFiles: MutableMap<String, GeneratedFileInfo> = mutableMapOf()
    )
    
    data class GeneratedFileInfo(
        val atomicFilePath: String,
        val generatedFilePath: String,
        val directory: String,
        val className: String,
        val lastModified: Long
    )
    
    private var myState = State()
    
    companion object {
        fun getInstance(project: Project): GeneratedFileTracker {
            return project.getService(GeneratedFileTracker::class.java)
        }
    }
    
    override fun getState(): State = myState
    
    override fun loadState(state: State) {
        myState = state
    }
    
    /**
     * Track a newly generated file
     */
    fun trackGeneratedFile(
        atomicFile: VirtualFile,
        generatedFile: VirtualFile,
        directory: String,
        className: String
    ) {
        val atomicPath = atomicFile.path
        val generatedPath = generatedFile.path
        
        
        val oldInfo = myState.trackedFiles[atomicPath]
        if (oldInfo != null && oldInfo.generatedFilePath != generatedPath) {
            
            val settings = AtomicPluginSettings.getInstance(project)
            if (settings.deleteOldFilesOnDirectoryChange) {
                deleteOldGeneratedFile(oldInfo.generatedFilePath)
            }
        }
        
        
        myState.trackedFiles[atomicPath] = GeneratedFileInfo(
            atomicFilePath = atomicPath,
            generatedFilePath = generatedPath,
            directory = directory,
            className = className,
            lastModified = System.currentTimeMillis()
        )
    }
    
    fun getGeneratedFileInfo(atomicFile: VirtualFile): GeneratedFileInfo? {
        return myState.trackedFiles[atomicFile.path]
    }

    fun hasOutputChanged(atomicFile: VirtualFile, newDirectory: String, newClassName: String): Boolean {
        val info = myState.trackedFiles[atomicFile.path] ?: return true
        return info.directory != newDirectory || info.className != newClassName
    }

    fun removeTracking(atomicFile: VirtualFile) {
        val info = myState.trackedFiles.remove(atomicFile.path)
        if (info != null) {
            deleteOldGeneratedFile(info.generatedFilePath)
        }
    }

    private fun deleteOldGeneratedFile(filePath: String) {
        val fileUrl = VirtualFileManager.constructUrl("file", filePath)
        val file = VirtualFileManager.getInstance().findFileByUrl(fileUrl)
        
        if (file != null && file.exists()) {
            try {
                file.delete(this)
            } catch (e: Exception) {
                
                println("Failed to delete old generated file: $filePath - ${e.message}")
            }
        }
    }

    fun cleanupOrphanedFiles() {
        val toRemove = mutableListOf<String>()
        
        myState.trackedFiles.forEach { (atomicPath, info) ->
            val atomicFileUrl = VirtualFileManager.constructUrl("file", atomicPath)
            val atomicFile = VirtualFileManager.getInstance().findFileByUrl(atomicFileUrl)
            
            if (atomicFile == null || !atomicFile.exists()) {
                
                deleteOldGeneratedFile(info.generatedFilePath)
                toRemove.add(atomicPath)
            }
        }
        
        toRemove.forEach { myState.trackedFiles.remove(it) }
    }
}