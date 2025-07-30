package com.jetbrains.rider.plugins.atomic.language

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.jetbrains.rider.plugins.atomic.psi.AtomicFile

class UseFullyQualifiedTypeQuickFix(
    private val typeName: String,
    private val namespace: String
) : IntentionAction {
    
    override fun getText(): String = "Use '$namespace.$typeName'"
    
    override fun getFamilyName(): String = "Use fully qualified type name"
    
    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return file is AtomicFile
    }
    
    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (editor == null || file !is AtomicFile) return
        
        WriteCommandAction.runWriteCommandAction(project, "Use Fully Qualified Type", null, Runnable {
            val document = editor.document
            val caretOffset = editor.caretModel.offset
            
            
            val text = document.text
            val lineStart = text.lastIndexOf('\n', caretOffset - 1) + 1
            val lineEnd = text.indexOf('\n', caretOffset).let { if (it == -1) text.length else it }
            val line = text.substring(lineStart, lineEnd)
            
            
            val typeIndex = line.indexOf(typeName)
            if (typeIndex != -1) {
                val absoluteStart = lineStart + typeIndex
                val absoluteEnd = absoluteStart + typeName.length
                
                
                document.replaceString(absoluteStart, absoluteEnd, "$namespace.$typeName")
            }
            
            
            PsiDocumentManager.getInstance(project).commitDocument(document)
        })
    }
    
    override fun startInWriteAction(): Boolean = false
}