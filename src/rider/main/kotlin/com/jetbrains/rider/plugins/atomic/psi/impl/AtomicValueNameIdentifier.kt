package com.jetbrains.rider.plugins.atomic.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.jetbrains.rider.plugins.atomic.psi.AtomicValueItem

class AtomicValueNameIdentifier(node: ASTNode) : LeafPsiElement(node.elementType, node.text), PsiNameIdentifierOwner {
    
    override fun getName(): String? = text
    
    override fun setName(name: String): PsiElement {
        throw UnsupportedOperationException("Renaming atomic values is not yet supported")
    }
    
    override fun getNameIdentifier(): PsiElement? = this
    
    override fun getTextOffset(): Int {
        return super.getTextOffset()
    }
    
    fun getValueItem(): AtomicValueItem? {
        return parent as? AtomicValueItem
    }
}