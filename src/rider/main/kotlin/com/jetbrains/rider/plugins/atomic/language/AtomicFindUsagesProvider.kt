package com.jetbrains.rider.plugins.atomic.language

import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement
import com.jetbrains.rider.plugins.atomic.psi.AtomicTagItem
import com.jetbrains.rider.plugins.atomic.psi.AtomicValueItem

class AtomicFindUsagesProvider : FindUsagesProvider {
    
    override fun getWordsScanner(): WordsScanner? {
        
        return null
    }
    
    override fun canFindUsagesFor(psiElement: PsiElement): Boolean {
        
        return false
    }
    
    override fun getHelpId(psiElement: PsiElement): String? {
        return null
    }
    
    override fun getType(element: PsiElement): String {
        return when (element) {
            is AtomicTagItem -> "tag"
            is AtomicValueItem -> "value"
            else -> ""
        }
    }
    
    override fun getDescriptiveName(element: PsiElement): String {
        return when (element) {
            is AtomicTagItem -> element.name ?: ""
            is AtomicValueItem -> element.name ?: ""
            else -> ""
        }
    }
    
    override fun getNodeText(element: PsiElement, useFullName: Boolean): String {
        return when (element) {
            is AtomicTagItem -> element.name ?: ""
            is AtomicValueItem -> element.name ?: ""
            else -> ""
        }
    }
}