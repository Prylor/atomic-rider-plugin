package com.jetbrains.rider.plugins.atomic.psi.impl

import com.intellij.openapi.util.TextRange
import com.intellij.psi.ElementManipulator
import com.intellij.psi.PsiElement
import com.intellij.util.IncorrectOperationException

class AtomicValueNameManipulator : ElementManipulator<PsiElement> {
    override fun handleContentChange(element: PsiElement, range: TextRange, newContent: String): PsiElement {
        throw IncorrectOperationException("Renaming atomic values is not yet supported")
    }

    override fun handleContentChange(element: PsiElement, newContent: String): PsiElement {
        throw IncorrectOperationException("Renaming atomic values is not yet supported")
    }

    override fun getRangeInElement(element: PsiElement): TextRange {
        return TextRange.allOf(element.text)
    }
}