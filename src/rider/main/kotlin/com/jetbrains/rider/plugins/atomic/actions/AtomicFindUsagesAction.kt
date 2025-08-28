package com.jetbrains.rider.plugins.atomic.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.usageView.UsageInfo
import com.intellij.usages.*
import com.jetbrains.rider.plugins.atomic.psi.AtomicFile
import com.jetbrains.rider.plugins.atomic.psi.AtomicValueItem
import com.jetbrains.rider.plugins.atomic.psi.AtomicTypes
import com.jetbrains.rider.plugins.atomic.services.AtomicGenerationService
import kotlinx.coroutines.runBlocking

class AtomicFindUsagesAction : AnAction("Find Usages of Generated Methods"), DumbAware {
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return
        
        if (psiFile !is AtomicFile) return
        
        val offset = editor.caretModel.offset
        val element = psiFile.findElementAt(offset) ?: return
        
        
        val valueElement = findValueElement(element) ?: return
        val valueName = getValueName(valueElement) ?: return
        
        
        showUsages(project, valueName, psiFile)
    }
    
    override fun update(e: AnActionEvent) {
        val project = e.project
        val editor = e.getData(CommonDataKeys.EDITOR)
        val psiFile = e.getData(CommonDataKeys.PSI_FILE)
        
        if (project == null || editor == null || psiFile !is AtomicFile) {
            e.presentation.isEnabledAndVisible = false
            return
        }
        
        val offset = editor.caretModel.offset
        val element = psiFile.findElementAt(offset)
        val valueElement = element?.let { findValueElement(it) }
        
        e.presentation.isEnabledAndVisible = valueElement != null
    }
    
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
    
    private fun findValueElement(element: PsiElement): PsiElement? {
        var current: PsiElement? = element
        
        while (current != null) {
            when {
                current.node?.elementType == AtomicTypes.VALUE_NAME -> return current
                current is AtomicValueItem -> return current
            }
            current = current.parent
        }
        
        return null
    }
    
    private fun getValueName(element: PsiElement): String? {
        return when {
            element.node?.elementType == AtomicTypes.VALUE_NAME -> element.text
            element is AtomicValueItem -> {
                val valueNameNode = element.node.findChildByType(AtomicTypes.VALUE_NAME)
                valueNameNode?.text
            }
            else -> null
        }
    }
    
    private fun showUsages(project: com.intellij.openapi.project.Project, valueName: String, atomicFile: AtomicFile) {
        val service = AtomicGenerationService.getInstance(project)
        val methodNames = listOf(
            "Get$valueName",
            "Set$valueName", 
            "Add$valueName",
            "Has$valueName",
            "Del$valueName",
            "TryGet$valueName",
            "Ref$valueName"
        )
        
        
        val generatedFilePath = service.calculateGeneratedFilePath(atomicFile)
        if (generatedFilePath == null) {
            
            return
        }
        
        
        val usageLocations = runBlocking {
            service.findGeneratedMethodUsages(valueName, methodNames, project.basePath ?: "", generatedFilePath)
        }
        
        
        val usageViewManager = UsageViewManager.getInstance(project)
        val presentation = UsageViewPresentation().apply {
            tabText = "Usages of $valueName"
            targetsNodeText = "Atomic value '$valueName'"
            isOpenInNewTab = false
            isShowCancelButton = true
        }
        
        
        val usages = usageLocations.mapNotNull { location ->
            val virtualFile = com.intellij.openapi.vfs.LocalFileSystem.getInstance().findFileByPath(location.filePath)
            if (virtualFile != null) {
                val psiFile = com.intellij.psi.PsiManager.getInstance(project).findFile(virtualFile)
                if (psiFile != null) {
                    val document = com.intellij.psi.PsiDocumentManager.getInstance(project).getDocument(psiFile)
                    if (document != null) {
                        val offset = document.getLineStartOffset(location.line - 1) + location.column - 1
                        val element = psiFile.findElementAt(offset)
                        if (element != null) {
                            UsageInfo2UsageAdapter(UsageInfo(element))
                        } else null
                    } else null
                } else null
            } else null
        }.toTypedArray()
        
        usageViewManager.showUsages(
            arrayOf<UsageTarget>(), 
            usages,
            presentation
        )
    }
}