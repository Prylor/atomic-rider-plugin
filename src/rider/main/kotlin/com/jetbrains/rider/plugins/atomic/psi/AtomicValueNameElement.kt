package com.jetbrains.rider.plugins.atomic.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.tree.IElementType

class AtomicValueNameElement(type: IElementType, text: CharSequence) : LeafPsiElement(type, text), PsiNamedElement {
    
    override fun getName(): String? = text
    
    override fun setName(name: String): PsiElement {
        throw UnsupportedOperationException("Renaming atomic values is not yet supported")
    }
}