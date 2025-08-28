package com.jetbrains.rider.plugins.atomic.language

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.FoldingGroup
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.rider.plugins.atomic.psi.*

class AtomicFoldingBuilder : FoldingBuilderEx(), DumbAware {
    
    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        val descriptors = mutableListOf<FoldingDescriptor>()
        
        if (root !is AtomicFile) return emptyArray()
        
        
        val headerSection = PsiTreeUtil.findChildOfType(root, AtomicHeaderSection::class.java)
        headerSection?.let {
            val propertyCount = it.aggressiveInliningPropList.size +
                               it.classNamePropList.size +
                               it.directoryPropList.size +
                               it.entityTypePropList.size +
                               it.headerPropList.size +
                               it.namespacePropList.size
            if (propertyCount > 1) {
                addFoldingDescriptor(descriptors, it, "header: ...")
            }
        }
        
        
        val importsSection = PsiTreeUtil.findChildOfType(root, AtomicImportsSection::class.java)
        importsSection?.let {
            if (it.importItemList.isNotEmpty()) {
                addFoldingDescriptor(descriptors, it, "imports: ${it.importItemList.size} items")
            }
        }
        
        
        val tagsSection = PsiTreeUtil.findChildOfType(root, AtomicTagsSection::class.java)
        tagsSection?.let {
            if (it.tagItemList.isNotEmpty()) {
                addFoldingDescriptor(descriptors, it, "tags: ${it.tagItemList.size} items")
            }
        }
        
        
        val valuesSection = PsiTreeUtil.findChildOfType(root, AtomicValuesSection::class.java)
        valuesSection?.let {
            if (it.valueItemList.isNotEmpty()) {
                addFoldingDescriptor(descriptors, it, "values: ${it.valueItemList.size} items")
            }
        }
        
        return descriptors.toTypedArray()
    }
    
    override fun getPlaceholderText(node: ASTNode): String? {
        return when (val psi = node.psi) {
            is AtomicHeaderSection -> "header: ..."
            is AtomicImportsSection -> "imports: ${psi.importItemList.size} items"
            is AtomicTagsSection -> "tags: ${psi.tagItemList.size} items"
            is AtomicValuesSection -> "values: ${psi.valueItemList.size} items"
            else -> "..."
        }
    }
    
    override fun isCollapsedByDefault(node: ASTNode): Boolean = false
    
    private fun addFoldingDescriptor(
        descriptors: MutableList<FoldingDescriptor>,
        element: PsiElement,
        placeholderText: String
    ) {
        val textRange = element.textRange
        if (textRange.length > 0) {
            descriptors.add(
                FoldingDescriptor(
                    element.node,
                    textRange,
                    FoldingGroup.newGroup(element.javaClass.simpleName),
                    placeholderText
                )
            )
        }
    }
}