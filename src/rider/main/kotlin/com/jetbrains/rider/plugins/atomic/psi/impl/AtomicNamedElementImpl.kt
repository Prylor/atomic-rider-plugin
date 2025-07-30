package com.jetbrains.rider.plugins.atomic.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.jetbrains.rider.plugins.atomic.psi.AtomicTypes

abstract class AtomicNamedElementImpl(node: ASTNode) : ASTWrapperPsiElement(node), PsiNamedElement {
    
    override fun getName(): String? {
        return findChildByType<PsiElement>(AtomicTypes.VALUE_NAME)?.text
    }
    
    override fun setName(name: String): PsiElement {
        throw UnsupportedOperationException("Renaming is not yet supported")
    }
    
    override fun getTextOffset(): Int {
        val nameElement = findChildByType<PsiElement>(AtomicTypes.VALUE_NAME)
        return nameElement?.textOffset ?: super.getTextOffset()
    }
}