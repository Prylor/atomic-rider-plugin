package com.jetbrains.rider.plugins.atomic.services

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.jetbrains.rider.plugins.atomic.settings.AtomicPluginSettings
import kotlinx.coroutines.*
import java.io.File
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
            // Note: Automatic deletion of old files is disabled
            // Users must manually delete old generated files when output path changes
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
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val file = File(filePath)
                if (file.exists()) {
                    if (file.delete()) {
                        println("Deleted old generated file using File API: $filePath")
                        return@launch
                    }
                }
                
                ApplicationManager.getApplication().invokeLater {
                    runWriteAction {
                        try {
                            val fileUrl = VirtualFileManager.constructUrl("file", filePath)
                            val virtualFile = VirtualFileManager.getInstance().findFileByUrl(fileUrl)
                            if (virtualFile != null && virtualFile.exists()) {
                                virtualFile.delete(this@GeneratedFileTracker)
                                println("Deleted old generated file using VirtualFile API: $filePath")
                            }
                        } catch (e: Exception) {
                            println("Failed to delete old generated file: $filePath - ${e.message}")
                        }
                    }
                }
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