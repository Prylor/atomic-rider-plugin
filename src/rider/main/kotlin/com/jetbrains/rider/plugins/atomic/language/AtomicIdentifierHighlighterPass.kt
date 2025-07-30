package com.jetbrains.rider.plugins.atomic.language

import com.intellij.codeInsight.highlighting.HighlightUsagesHandlerBase
import com.intellij.codeInsight.highlighting.HighlightUsagesHandlerFactory
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.Consumer
import com.jetbrains.rider.plugins.atomic.psi.AtomicFile
import com.jetbrains.rider.plugins.atomic.psi.AtomicTagItem

class AtomicHighlightUsagesHandlerFactory : HighlightUsagesHandlerFactory {
    override fun createHighlightUsagesHandler(editor: Editor, file: PsiFile): HighlightUsagesHandlerBase<*>? {
        if (file !is AtomicFile) return null
        
        val offset = editor.caretModel.offset
        val element = file.findElementAt(offset) ?: return null
        
        
        val tag = PsiTreeUtil.getParentOfType(element, AtomicTagItem::class.java, false)
        if (tag != null) {
            return AtomicTagHighlightUsagesHandler(editor, file, tag)
        }
        
        return null
    }
}

class AtomicTagHighlightUsagesHandler(
    editor: Editor,
    file: PsiFile,
    private val tag: AtomicTagItem
) : HighlightUsagesHandlerBase<PsiElement>(editor, file) {
    
    override fun getTargets(): List<PsiElement> {
        
        return listOf(tag)
    }
    
    override fun selectTargets(
        targets: List<out PsiElement>,
        selectionConsumer: Consumer<in List<out PsiElement>>
    ) {
        selectionConsumer.consume(targets)
    }
    
    override fun computeUsages(targets: List<PsiElement>) {
        
        for (target in targets) {
            if (target is AtomicTagItem) {
                target.nameIdentifier?.let { addOccurrence(it) }
            }
        }
    }
}