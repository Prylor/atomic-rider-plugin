package com.jetbrains.rider.plugins.atomic.refactoring

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiReference
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.listeners.RefactoringElementListener
import com.intellij.refactoring.rename.RenameProcessor
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import com.intellij.usageView.UsageInfo
import com.jetbrains.rider.plugins.atomic.psi.AtomicFile
import com.jetbrains.rider.plugins.atomic.psi.AtomicTagItem
import com.jetbrains.rider.plugins.atomic.psi.AtomicValueItem
import com.jetbrains.rider.plugins.atomic.psi.AtomicTypes
import com.jetbrains.rider.plugins.atomic.services.AtomicGenerationService
import kotlinx.coroutines.*

class AtomicRenameProcessor : RenamePsiElementProcessor() {
    companion object {
        private val logger = Logger.getInstance(AtomicRenameProcessor::class.java)
    }
    
    override fun canProcessElement(element: PsiElement): Boolean {
        return when {
            element is AtomicValueItem -> true
            element is AtomicTagItem -> true
            element.parent is AtomicValueItem && element.node?.elementType == AtomicTypes.VALUE_NAME -> true
            element.parent is AtomicTagItem && element.node?.elementType == AtomicTypes.TAG_NAME -> true
            else -> false
        }
    }
    
    override fun renameElement(
        element: PsiElement,
        newName: String,
        usages: Array<UsageInfo>,
        listener: RefactoringElementListener?
    ) {
        logger.info("AtomicRenameProcessor: Starting rename of ${element.text} to $newName")
        
        val project = element.project
        val atomicFile = element.containingFile as? AtomicFile
        if (atomicFile == null) {
            logger.error("AtomicRenameProcessor: Element is not in an atomic file")
            return
        }
        
        val (actualElement, oldName) = when {
            element is AtomicValueItem -> {
                val valueNameNode = element.node.findChildByType(AtomicTypes.VALUE_NAME)
                Pair(element, valueNameNode?.text ?: return)
            }
            element is AtomicTagItem -> {
                val tagNameNode = element.node.findChildByType(AtomicTypes.TAG_NAME)
                Pair(element, tagNameNode?.text ?: return)
            }
            element.parent is AtomicValueItem -> {
                Pair(element.parent as AtomicValueItem, element.text)
            }
            element.parent is AtomicTagItem -> {
                Pair(element.parent as AtomicTagItem, element.text)
            }
            else -> {
                logger.error("AtomicRenameProcessor: Unexpected element type")
                return
            }
        }
        
        logger.info("AtomicRenameProcessor: Actual element type: ${actualElement.javaClass.simpleName}, old name: '$oldName', new name: '$newName'")
        
        val sanitizedNewName = newName.replace(" ", "").replace(Regex("[^a-zA-Z0-9_]"), "")
        if (sanitizedNewName != newName) {
            logger.info("AtomicRenameProcessor: Sanitized name from '$newName' to '$sanitizedNewName'")
        }
        
        logger.info("AtomicRenameProcessor: Calling super.renameElement to handle PSI update")
        super.renameElement(element, sanitizedNewName, usages, listener)
        logger.info("AtomicRenameProcessor: super.renameElement completed")
        
        val document = com.intellij.psi.PsiDocumentManager.getInstance(project).getDocument(atomicFile)
        if (document != null) {
            val psiDocManager = com.intellij.psi.PsiDocumentManager.getInstance(project)
            psiDocManager.commitDocument(document)
            logger.info("AtomicRenameProcessor: Committed document changes")
            
            logger.info("AtomicRenameProcessor: Document content after commit:")
            val lines = document.text.lines().take(20)
            lines.forEach { line ->
                logger.info("  $line")
            }
            
            val fileDocumentManager = com.intellij.openapi.fileEditor.FileDocumentManager.getInstance()
            fileDocumentManager.saveDocument(document)
            logger.info("AtomicRenameProcessor: Saved document to disk")
            
            atomicFile.virtualFile.refresh(false, false)
            logger.info("AtomicRenameProcessor: Refreshed virtual file")
        }
        
        GlobalScope.launch(Dispatchers.IO) {
            try {
                delay(500)
                
                val service = AtomicGenerationService.getInstance(project)
                
                when (actualElement) {
                    is AtomicValueItem -> {
                        logger.info("AtomicRenameProcessor: Backend rename value from $oldName to $sanitizedNewName")
                        try {
                            val response = service.renameValue(
                                atomicFile.virtualFile.path,
                                oldName,
                                sanitizedNewName,
                                project.basePath ?: ""
                            )
                            
                            if (response.success) {
                                logger.info("AtomicRenameProcessor: Backend successfully updated ${response.updatedUsages.size} usages")
                            } else {
                                logger.error("AtomicRenameProcessor: Backend rename failed: ${response.errorMessage}")
                            }
                        } catch (e: Exception) {
                            logger.warn("AtomicRenameProcessor: Backend rename timed out or failed, but frontend rename succeeded", e)
                        }
                    }
                    is AtomicTagItem -> {
                        logger.info("AtomicRenameProcessor: Backend rename tag from $oldName to $sanitizedNewName")
                        try {
                            val response = service.renameTag(
                                atomicFile.virtualFile.path,
                                oldName,
                                sanitizedNewName,
                                project.basePath ?: ""
                            )
                            
                            if (response.success) {
                                logger.info("AtomicRenameProcessor: Backend successfully updated ${response.updatedUsages.size} usages")
                            } else {
                                logger.error("AtomicRenameProcessor: Backend rename failed: ${response.errorMessage}")
                            }
                        } catch (e: Exception) {
                            
                            logger.warn("AtomicRenameProcessor: Backend rename timed out or failed, but frontend rename succeeded", e)
                        }
                    }
                }
                
                delay(1000) 
                ApplicationManager.getApplication().invokeLater {
                    atomicFile.virtualFile.parent?.refresh(false, true)
                }
                
            } catch (e: Exception) {
                logger.error("AtomicRenameProcessor: Error during backend update", e)
                
            }
        }
    }
    
    override fun prepareRenaming(
        element: PsiElement,
        newName: String,
        allRenames: MutableMap<PsiElement, String>,
        scope: com.intellij.psi.search.SearchScope
    ) {
        
        val elementToRename = when {
            element is AtomicValueItem || element is AtomicTagItem -> element
            element.parent is AtomicValueItem -> element.parent as AtomicValueItem
            element.parent is AtomicTagItem -> element.parent as AtomicTagItem
            else -> return
        }
        
        allRenames[elementToRename] = newName
        
        logger.info("AtomicRenameProcessor: Prepared renaming for ${elementToRename.javaClass.simpleName}")
    }
    
    override fun findReferences(
        element: PsiElement,
        searchScope: com.intellij.psi.search.SearchScope,
        searchInCommentsAndStrings: Boolean
    ): MutableCollection<PsiReference> {
        
        
        return mutableListOf()
    }
}