package com.jetbrains.rider.plugins.atomic.startup

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.jetbrains.rider.plugins.atomic.psi.AtomicFile
import com.jetbrains.rider.plugins.atomic.services.AtomicGenerationService
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AtomicStartupActivity : StartupActivity.DumbAware {
    
    override fun runActivity(project: Project) {
        GlobalScope.launch {
            val service = AtomicGenerationService.getInstance(project)
            var attempts = 0
            val maxAttempts = 30 
            
            while (!service.isReady() && attempts < maxAttempts) {
                delay(1000) 
                attempts++
            }
            
            if (service.isReady()) {
                
                val fileEditorManager = FileEditorManager.getInstance(project)
                val openFiles = fileEditorManager.openFiles
                
                com.intellij.openapi.application.ReadAction.run<Exception> {
                    for (virtualFile in openFiles) {
                        if (virtualFile.extension == "atomic") {
                            val psiFile = com.intellij.psi.PsiManager.getInstance(project).findFile(virtualFile)
                            if (psiFile is AtomicFile) {
                                DaemonCodeAnalyzer.getInstance(project).restart(psiFile)
                            }
                        }
                    }
                }
            }
        }
    }
}