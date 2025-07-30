package com.jetbrains.rider.plugins.atomic.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiReference
import com.jetbrains.rider.plugins.atomic.psi.AtomicTypes
import com.jetbrains.rider.plugins.atomic.psi.AtomicTagItem

abstract class AtomicTagItemImplExt(node: ASTNode) : ASTWrapperPsiElement(node), AtomicTagItem, PsiNameIdentifierOwner {
    
    override fun getName(): String? {
        return nameIdentifier?.text
    }
    
    override fun setName(newName: String): PsiElement {
        val nameNode = node.findChildByType(AtomicTypes.TAG_NAME)
        if (nameNode != null) {
            val document = com.intellij.psi.PsiDocumentManager.getInstance(project).getDocument(containingFile)
            if (document != null) {
                val startOffset = nameNode.startOffset
                val endOffset = startOffset + nameNode.textLength
                document.replaceString(startOffset, endOffset, newName)
                com.intellij.psi.PsiDocumentManager.getInstance(project).commitDocument(document)
            }
        }
        return this
    }
    
    override fun getNameIdentifier(): PsiElement? {
        return node.findChildByType(AtomicTypes.TAG_NAME)?.psi
    }
    
    override fun getTextOffset(): Int {
        val tagNameNode = node.findChildByType(AtomicTypes.TAG_NAME)
        return tagNameNode?.textRange?.startOffset ?: super.getTextOffset()
    }
    
    override fun getReference(): PsiReference? {
        return null
    }
    
    override fun getReferences(): Array<PsiReference> {
        return PsiReference.EMPTY_ARRAY
    }
}