package com.jetbrains.rider.plugins.atomic.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.jetbrains.rider.plugins.atomic.psi.AtomicTypes
import com.jetbrains.rider.plugins.atomic.psi.AtomicValueItem

abstract class AtomicValueItemImplExt(node: ASTNode) : ASTWrapperPsiElement(node), AtomicValueItem, PsiNameIdentifierOwner {
    companion object {
        private val logger = Logger.getInstance(AtomicValueItemImplExt::class.java)
    }
    
    override fun getName(): String? {
        return nameIdentifier?.text
    }
    
    override fun setName(newName: String): PsiElement {
        logger.info("AtomicValueItemImplExt.setName called with newName='$newName'")
        val nameNode = node.findChildByType(AtomicTypes.VALUE_NAME)
        if (nameNode != null) {
            logger.info("AtomicValueItemImplExt: Found VALUE_NAME node with text='${nameNode.text}', type: ${nameNode.javaClass.simpleName}")
            
            val document = com.intellij.psi.PsiDocumentManager.getInstance(project).getDocument(containingFile)
            if (document != null) {
                val startOffset = nameNode.startOffset
                val endOffset = startOffset + nameNode.textLength
                logger.info("AtomicValueItemImplExt: Updating document text from offset $startOffset to $endOffset")
                logger.info("AtomicValueItemImplExt: Old text: '${document.getText(com.intellij.openapi.util.TextRange(startOffset, endOffset))}'")
                
                document.replaceString(startOffset, endOffset, newName)
                logger.info("AtomicValueItemImplExt: Document updated with new name: '$newName'")
                
                com.intellij.psi.PsiDocumentManager.getInstance(project).commitDocument(document)
                logger.info("AtomicValueItemImplExt: Document committed")
            } else {
                logger.error("AtomicValueItemImplExt: Could not get document for file")
            }
        } else {
            logger.error("AtomicValueItemImplExt: VALUE_NAME node not found")
        }
        return this
    }
    
    override fun getNameIdentifier(): PsiElement? {
        return node.findChildByType(AtomicTypes.VALUE_NAME)?.psi
    }
    
    override fun getTextOffset(): Int {
        val nameNode = nameIdentifier
        return nameNode?.textOffset ?: super.getTextOffset()
    }
}