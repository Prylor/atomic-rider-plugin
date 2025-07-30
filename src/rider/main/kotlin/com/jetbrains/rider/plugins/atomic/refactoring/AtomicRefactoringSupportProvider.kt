package com.jetbrains.rider.plugins.atomic.refactoring

import com.intellij.lang.refactoring.RefactoringSupportProvider
import com.intellij.psi.PsiElement
import com.jetbrains.rider.plugins.atomic.psi.AtomicTagItem
import com.jetbrains.rider.plugins.atomic.psi.AtomicValueItem

class AtomicRefactoringSupportProvider : RefactoringSupportProvider() {
    
    override fun isMemberInplaceRenameAvailable(element: PsiElement, context: PsiElement?): Boolean {
        return when (element) {
            is AtomicValueItem -> true
            is AtomicTagItem -> true
            else -> {
                var parent = element.parent
                while (parent != null && parent !is com.intellij.psi.PsiFile) {
                    if (parent is AtomicValueItem || parent is AtomicTagItem) {
                        return true
                    }
                    parent = parent.parent
                }
                false
            }
        }
    }
    
    override fun isInplaceRenameAvailable(element: PsiElement, context: PsiElement?): Boolean {
        return isMemberInplaceRenameAvailable(element, context)
    }
}