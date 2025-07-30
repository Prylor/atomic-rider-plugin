package com.jetbrains.rider.plugins.atomic.language

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.jetbrains.rider.plugins.atomic.psi.AtomicFile
import com.jetbrains.rider.plugins.atomic.psi.AtomicImportsSection
import com.jetbrains.rider.plugins.atomic.psi.AtomicTypes

class RemoveImportQuickFix(private val namespace: String) : IntentionAction {
    
    override fun getText(): String = "Remove unused import '$namespace'"
    
    override fun getFamilyName(): String = "Remove unused import"
    
    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return file is AtomicFile
    }
    
    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (editor == null || file !is AtomicFile) return
        
        WriteCommandAction.runWriteCommandAction(project, "Remove Import", null, Runnable {
            val document = editor.document
            val importsSection = file.children.filterIsInstance<AtomicImportsSection>().firstOrNull() ?: return@Runnable
            
            
            val importToRemove = importsSection.importItemList.firstOrNull { importItem ->
                importItem.node.findChildByType(AtomicTypes.IMPORT_PATH)?.text == namespace
            } ?: return@Runnable
            
            
            val startOffset = importToRemove.textRange.startOffset
            var endOffset = importToRemove.textRange.endOffset
            
            
            val text = document.text
            if (endOffset < text.length && text[endOffset] == '\n') {
                endOffset++
            } else if (endOffset < text.length - 1 && text[endOffset] == '\r' && text[endOffset + 1] == '\n') {
                endOffset += 2
            }
            
            
            if (importsSection.importItemList.size == 1) {
                
                val importsKeyword = importsSection.node.findChildByType(AtomicTypes.IMPORTS_KEYWORD)
                if (importsKeyword != null) {
                    
                    val sectionStart = importsKeyword.textRange.startOffset
                    var sectionEnd = endOffset
                    
                    
                    while (sectionEnd < text.length && (text[sectionEnd] == '\n' || text[sectionEnd] == '\r')) {
                        sectionEnd++
                    }
                    
                    document.deleteString(sectionStart, sectionEnd)
                    
                    
                    if (sectionStart > 0 && text[sectionStart - 1] == '\n') {
                        var lineStart = sectionStart - 1
                        if (lineStart > 0 && text[lineStart - 1] == '\r') {
                            lineStart--
                        }
                        document.deleteString(lineStart, sectionStart)
                    }
                }
            } else {
                
                document.deleteString(startOffset, endOffset)
            }
            
            
            PsiDocumentManager.getInstance(project).commitDocument(document)
        })
    }
    
    override fun startInWriteAction(): Boolean = false
}