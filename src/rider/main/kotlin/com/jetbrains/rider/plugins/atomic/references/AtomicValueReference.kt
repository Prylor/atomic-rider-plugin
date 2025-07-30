package com.jetbrains.rider.plugins.atomic.references

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.util.IncorrectOperationException
import com.jetbrains.rider.plugins.atomic.psi.AtomicFile
import com.jetbrains.rider.plugins.atomic.psi.AtomicValueItem
import com.jetbrains.rider.plugins.atomic.psi.AtomicTypes
import com.jetbrains.rider.plugins.atomic.services.AtomicGenerationService
import kotlinx.coroutines.runBlocking

class AtomicValueReference(
    element: PsiElement,
    private val valueName: String
) : PsiReferenceBase<PsiElement>(element, TextRange.allOf(element.text)) {
    
    override fun resolve(): PsiElement? {
        
        return element
    }
    
    override fun getVariants(): Array<Any> = emptyArray()
    
    override fun handleElementRename(newElementName: String): PsiElement {
        return element
    }
    
    override fun isReferenceTo(element: PsiElement): Boolean {
        
        return element == this.element
    }
    
    private fun getGeneratedMethodNames(valueName: String): Set<String> {
        return setOf(
            "Get$valueName",
            "Set$valueName",
            "Add$valueName",
            "Has$valueName",
            "Del$valueName",
            "TryGet$valueName",
            "Ref$valueName" 
        )
    }
    
    fun findUsages(): Collection<PsiReference> {
        val project = element.project
        val service = AtomicGenerationService.getInstance(project)
        
        
        val atomicFile = element.containingFile as? AtomicFile ?: return emptyList()
        
        
        val scope = GlobalSearchScope.projectScope(project)
        val usages = mutableListOf<PsiReference>()
        
        
        val generatedFilePath = service.calculateGeneratedFilePath(atomicFile) ?: return usages
        
        
        val methodNames = getGeneratedMethodNames(valueName)
        
        
        val usageResults = runBlocking {
            service.findGeneratedMethodUsages(valueName, methodNames.toList(), project.basePath ?: "", generatedFilePath)
        }
        
        
        
        
        return usages
    }
}