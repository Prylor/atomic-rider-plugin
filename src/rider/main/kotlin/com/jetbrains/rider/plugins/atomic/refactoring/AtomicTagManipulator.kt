package com.jetbrains.rider.plugins.atomic.refactoring

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.TextRange
import com.intellij.psi.AbstractElementManipulator
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.jetbrains.rider.plugins.atomic.psi.AtomicTagItem
import com.jetbrains.rider.plugins.atomic.psi.AtomicTypes

class AtomicTagManipulator : AbstractElementManipulator<AtomicTagItem>() {
    companion object {
        private val logger = Logger.getInstance(AtomicTagManipulator::class.java)
    }
    
    override fun handleContentChange(element: AtomicTagItem, range: TextRange, newContent: String): AtomicTagItem {
        logger.info("AtomicTagManipulator: handleContentChange called with newContent='$newContent'")
        
        val tagNameNode = element.node.findChildByType(AtomicTypes.TAG_NAME)
        if (tagNameNode != null) {
            logger.info("AtomicTagManipulator: Found TAG_NAME node with text='${tagNameNode.text}'")
            
            val leafNode = tagNameNode.firstChildNode
            if (leafNode is LeafPsiElement) {
                logger.info("AtomicTagManipulator: Replacing leaf text from '${leafNode.text}' to '$newContent'")
                leafNode.replaceWithText(newContent)
                logger.info("AtomicTagManipulator: After replacement, TAG_NAME text='${tagNameNode.text}'")
            } else {
                logger.warn("AtomicTagManipulator: TAG_NAME firstChildNode is not a LeafPsiElement")
            }
        } else {
            logger.error("AtomicTagManipulator: TAG_NAME node not found in element")
        }
        
        logger.info("AtomicTagManipulator: After handleContentChange, element text='${element.text}'")
        return element
    }
    
    override fun getRangeInElement(element: AtomicTagItem): TextRange {
        val tagNameNode = element.node.findChildByType(AtomicTypes.TAG_NAME)
        if (tagNameNode != null) {
            val startOffset = tagNameNode.startOffset - element.node.startOffset
            return TextRange(startOffset, startOffset + tagNameNode.textLength)
        }
        return TextRange.EMPTY_RANGE
    }
}