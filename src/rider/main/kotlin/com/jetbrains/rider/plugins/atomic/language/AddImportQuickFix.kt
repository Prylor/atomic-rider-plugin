package com.jetbrains.rider.plugins.atomic.language

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.jetbrains.rider.plugins.atomic.psi.AtomicFile

class AddImportQuickFix(private val namespace: String) : IntentionAction {
    
    override fun getText(): String = "Import '$namespace'"
    
    override fun getFamilyName(): String = "Import namespace"
    
    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return file is AtomicFile
    }
    
    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (editor == null || file !is AtomicFile) return
        
        AtomicCompletionContributor.addImportToAtomicFile(project, editor.document, namespace)
    }
    
    override fun startInWriteAction(): Boolean = false
}