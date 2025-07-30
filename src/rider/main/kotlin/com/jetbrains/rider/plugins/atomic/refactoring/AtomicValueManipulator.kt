package com.jetbrains.rider.plugins.atomic.refactoring

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.TextRange
import com.intellij.psi.AbstractElementManipulator
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.jetbrains.rider.plugins.atomic.psi.AtomicFile
import com.jetbrains.rider.plugins.atomic.psi.AtomicValueItem
import com.jetbrains.rider.plugins.atomic.psi.AtomicTypes
import com.jetbrains.rider.plugins.atomic.psi.impl.AtomicPsiImplUtil

class AtomicValueManipulator : AbstractElementManipulator<AtomicValueItem>() {
    companion object {
        private val logger = Logger.getInstance(AtomicValueManipulator::class.java)
    }
    
    override fun handleContentChange(element: AtomicValueItem, range: TextRange, newContent: String): AtomicValueItem {
        logger.info("AtomicValueManipulator: handleContentChange called with newContent='$newContent'")
        
        val valueNameNode = element.node.findChildByType(AtomicTypes.VALUE_NAME)
        if (valueNameNode != null) {
            logger.info("AtomicValueManipulator: Found VALUE_NAME node with text='${valueNameNode.text}'")
            
            // Replace the VALUE_NAME node's text
            val leafNode = valueNameNode.firstChildNode
            if (leafNode is LeafPsiElement) {
                logger.info("AtomicValueManipulator: Replacing leaf text from '${leafNode.text}' to '$newContent'")
                leafNode.replaceWithText(newContent)
                logger.info("AtomicValueManipulator: After replacement, VALUE_NAME text='${valueNameNode.text}'")
            } else {
                logger.warn("AtomicValueManipulator: VALUE_NAME firstChildNode is not a LeafPsiElement")
            }
        } else {
            logger.error("AtomicValueManipulator: VALUE_NAME node not found in element")
        }
        
        logger.info("AtomicValueManipulator: After handleContentChange, element text='${element.text}'")
        return element
    }
    
    override fun getRangeInElement(element: AtomicValueItem): TextRange {
        val valueNameNode = element.node.findChildByType(AtomicTypes.VALUE_NAME)
        if (valueNameNode != null) {
            val startOffset = valueNameNode.startOffset - element.node.startOffset
            return TextRange(startOffset, startOffset + valueNameNode.textLength)
        }
        return TextRange.EMPTY_RANGE
    }
}