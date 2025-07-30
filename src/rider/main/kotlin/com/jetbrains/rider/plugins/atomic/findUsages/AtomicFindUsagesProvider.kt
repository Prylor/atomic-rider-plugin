package com.jetbrains.rider.plugins.atomic.findUsages

import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.tree.TokenSet
import com.jetbrains.rider.plugins.atomic.language.AtomicLexerAdapter
import com.jetbrains.rider.plugins.atomic.psi.AtomicTypes
import com.jetbrains.rider.plugins.atomic.psi.AtomicValueItem
import com.jetbrains.rider.plugins.atomic.psi.AtomicTagItem

class AtomicFindUsagesProvider : FindUsagesProvider {
    
    override fun getWordsScanner(): WordsScanner? {
        return DefaultWordsScanner(
            AtomicLexerAdapter(),
            TokenSet.create(AtomicTypes.VALUE_NAME, AtomicTypes.TAG_NAME),
            TokenSet.create(AtomicTypes.COMMENT),
            TokenSet.EMPTY
        )
    }
    
    override fun canFindUsagesFor(psiElement: PsiElement): Boolean {
        println("[AtomicFindUsagesProvider] canFindUsagesFor called:")
        println("  Element class: ${psiElement.javaClass.name}")
        println("  Element text: '${psiElement.text}'")
        println("  Node type: ${psiElement.node?.elementType}")
        println("  Parent class: ${psiElement.parent?.javaClass?.name}")
        
        
        if (psiElement.node?.elementType == AtomicTypes.VALUE_NAME && psiElement.parent is AtomicValueItem) {
            println("  -> Matched VALUE_NAME token")
            return true
        }
        
        
        if (psiElement.node?.elementType == AtomicTypes.TAG_NAME && psiElement.parent is AtomicTagItem) {
            println("  -> Matched TAG_NAME token")
            return true
        }
        
        
        if (psiElement is AtomicValueItem) {
            println("  -> Matched AtomicValueItem")
            return true
        }
        
        
        if (psiElement is AtomicTagItem) {
            println("  -> Matched AtomicTagItem")
            return true
        }
        
        
        var parent = psiElement.parent
        while (parent != null && parent !is com.intellij.psi.PsiFile) {
            if (parent is AtomicValueItem) {
                println("  -> Matched child of AtomicValueItem")
                return true
            }
            if (parent is AtomicTagItem) {
                println("  -> Matched child of AtomicTagItem")
                return true
            }
            parent = parent.parent
        }
        
        println("  -> No match")
        return false
    }
    
    override fun getHelpId(psiElement: PsiElement): String? = null
    
    override fun getType(element: PsiElement): String {
        return when {
            element.parent is AtomicValueItem -> "atomic value"
            element.parent is AtomicTagItem -> "atomic tag"
            element is AtomicValueItem -> "atomic value"
            element is AtomicTagItem -> "atomic tag"
            else -> ""
        }
    }
    
    override fun getDescriptiveName(element: PsiElement): String {
        return when {
            
            element.node?.elementType == AtomicTypes.VALUE_NAME -> element.text
            
            
            element.node?.elementType == AtomicTypes.TAG_NAME -> element.text
            
            
            element is AtomicValueItem -> {
                val valueNameNode = element.node.findChildByType(AtomicTypes.VALUE_NAME)
                valueNameNode?.text ?: element.text
            }
            
            
            element is AtomicTagItem -> {
                val tagNameNode = element.node.findChildByType(AtomicTypes.TAG_NAME)
                tagNameNode?.text ?: element.text
            }
            
            element is PsiNamedElement -> element.name ?: ""
            
            else -> element.text
        }
    }
    
    override fun getNodeText(element: PsiElement, useFullName: Boolean): String {
        return when {
            
            element.node?.elementType == AtomicTypes.VALUE_NAME -> {
                "Atomic value '${element.text}'"
            }
            
            
            element.node?.elementType == AtomicTypes.TAG_NAME -> {
                "Atomic tag '${element.text}'"
            }
            
            
            element is AtomicValueItem -> {
                val valueNameNode = element.node.findChildByType(AtomicTypes.VALUE_NAME)
                val valueName = valueNameNode?.text ?: ""
                if (valueName.isNotEmpty()) {
                    "Atomic value '$valueName'"
                } else {
                    element.text
                }
            }
            
            
            element is AtomicTagItem -> {
                val tagNameNode = element.node.findChildByType(AtomicTypes.TAG_NAME)
                val tagName = tagNameNode?.text ?: ""
                if (tagName.isNotEmpty()) {
                    "Atomic tag '$tagName'"
                } else {
                    element.text
                }
            }
            
            else -> element.text
        }
    }
}