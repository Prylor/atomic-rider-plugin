package com.jetbrains.rider.plugins.atomic.findUsages

import com.intellij.psi.ElementDescriptionLocation
import com.intellij.psi.ElementDescriptionProvider
import com.intellij.psi.PsiElement
import com.intellij.usageView.UsageViewLongNameLocation
import com.intellij.usageView.UsageViewNodeTextLocation
import com.intellij.usageView.UsageViewShortNameLocation
import com.intellij.usageView.UsageViewTypeLocation
import com.jetbrains.rider.plugins.atomic.psi.AtomicValueItem
import com.jetbrains.rider.plugins.atomic.psi.AtomicTagItem
import com.jetbrains.rider.plugins.atomic.psi.AtomicTypes

class AtomicElementDescriptionProvider : ElementDescriptionProvider {
    
    override fun getElementDescription(element: PsiElement, location: ElementDescriptionLocation): String? {
        
        val valueName = when {
            element.node?.elementType == AtomicTypes.VALUE_NAME -> element.text
            element is AtomicValueItem -> element.node.findChildByType(AtomicTypes.VALUE_NAME)?.text
            else -> {
                
                var parent = element.parent
                while (parent != null && parent !is com.intellij.psi.PsiFile) {
                    if (parent is AtomicValueItem) {
                        return getElementDescription(parent, location)
                    }
                    parent = parent.parent
                }
                null
            }
        }
        
        if (valueName != null) {
            return when (location) {
                is UsageViewTypeLocation -> "atomic value"
                is UsageViewNodeTextLocation -> "Atomic value '$valueName'"
                is UsageViewShortNameLocation -> valueName
                is UsageViewLongNameLocation -> "Atomic value '$valueName'"
                else -> valueName
            }
        }
        
        
        val tagName = when {
            element.node?.elementType == AtomicTypes.TAG_NAME -> element.text
            element is AtomicTagItem -> element.node.findChildByType(AtomicTypes.TAG_NAME)?.text
            else -> {
                
                var parent = element.parent
                while (parent != null && parent !is com.intellij.psi.PsiFile) {
                    if (parent is AtomicTagItem) {
                        return getElementDescription(parent, location)
                    }
                    parent = parent.parent
                }
                null
            }
        }
        
        if (tagName != null) {
            return when (location) {
                is UsageViewTypeLocation -> "atomic tag"
                is UsageViewNodeTextLocation -> "Atomic tag '$tagName'"
                is UsageViewShortNameLocation -> tagName
                is UsageViewLongNameLocation -> "Atomic tag '$tagName'"
                else -> tagName
            }
        }
        
        return null
    }
}