package com.jetbrains.rider.plugins.atomic.search

import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.openapi.application.ReadAction
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.UsageSearchContext
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.util.Processor
import com.jetbrains.rider.plugins.atomic.psi.AtomicFile
import com.jetbrains.rider.plugins.atomic.psi.AtomicValueItem
import com.jetbrains.rider.plugins.atomic.psi.AtomicTypes
import com.jetbrains.rider.plugins.atomic.services.AtomicGenerationService
import kotlinx.coroutines.runBlocking
import com.intellij.psi.PsiManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.jetbrains.rider.plugins.atomic.references.AtomicValueReference

class AtomicValueUsageSearcher : QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters>(true) {
    
    override fun processQuery(parameters: ReferencesSearch.SearchParameters, consumer: Processor<in PsiReference>) {
        val element = parameters.elementToSearch
        
        println("[AtomicValueUsageSearcher] processQuery called:")
        println("  Element class: ${element.javaClass.name}")
        println("  Element text: '${element.text}'")
        println("  Node type: ${element.node?.elementType}")
        
        val valueName = when {
            element is AtomicValueItem -> {
                val valueNameNode = element.node.findChildByType(AtomicTypes.VALUE_NAME)
                println("  -> Element is AtomicValueItem, name: ${valueNameNode?.text}")
                valueNameNode?.text
            }
            
            element.node?.elementType == AtomicTypes.VALUE_NAME && element.parent is AtomicValueItem -> {
                println("  -> Element is VALUE_NAME token")
                element.text
            }
            
            element.parent?.node?.elementType == AtomicTypes.VALUE_NAME && element.parent?.parent is AtomicValueItem -> {
                println("  -> Element is leaf inside VALUE_NAME")
                element.parent.text
            }
            
            else -> {
                println("  -> Element type not recognized")
                null
            }
        }
        
        if (valueName != null && valueName.isNotEmpty()) {
            println("  -> Found value name: '$valueName'")
            val project = element.project
            
            val atomicFile = element.containingFile as? AtomicFile
            if (atomicFile == null) {
                println("  -> Element is not in an AtomicFile")
                return
            }
            
            ReadAction.run<RuntimeException> {
                val service = AtomicGenerationService.getInstance(project)
                
                
                val generatedFilePath = service.calculateGeneratedFilePath(atomicFile)
                if (generatedFilePath == null) {
                    println("  -> Could not calculate generated file path")
                    return@run
                }
                
                val methodNames = getGeneratedMethodNames(valueName)
                println("  -> Looking for methods: ${methodNames.joinToString()}")
                
                
                val usages = runBlocking {
                    service.findGeneratedMethodUsages(valueName, methodNames, project.basePath ?: "", generatedFilePath)
                }
                
                println("  -> Found ${usages.size} usages")
                usages.forEach { usage ->
                    println("    Usage: ${usage.methodName} at ${usage.filePath}:${usage.line}")
                }
                
                val psiManager = PsiManager.getInstance(project)
                
                for (usage in usages) {
                    val virtualFile = LocalFileSystem.getInstance().findFileByPath(usage.filePath)
                    if (virtualFile != null) {
                        val psiFile = psiManager.findFile(virtualFile)
                        if (psiFile != null) {
                            
                            val reference = SyntheticAtomicValueReference(element, psiFile, usage)
                            val processed = consumer.process(reference)
                            println("  -> Processed reference: $processed")
                        } else {
                            println("  -> Could not find PSI file for: ${usage.filePath}")
                        }
                    } else {
                        println("  -> Could not find virtual file for: ${usage.filePath}")
                    }
                }
            }
        } else {
            println("  -> No value name found")
        }
    }
    
    private fun getGeneratedMethodNames(valueName: String): List<String> {
        return listOf(
            "Get$valueName",
            "Set$valueName",
            "Add$valueName",
            "Has$valueName",
            "Del$valueName",
            "TryGet$valueName",
            "Ref$valueName"
        )
    }
}

/**
 * Синтетическая ссылка, представляющая использование сгенерированного метода.
 */
class SyntheticAtomicValueReference(
    private val atomicValueElement: PsiElement,
    private val usageFile: com.intellij.psi.PsiFile,
    private val usage: com.jetbrains.rider.plugins.atomic.model.MethodUsageLocation
) : PsiReference {
    
    override fun getElement(): PsiElement = usageFile
    
    override fun getRangeInElement(): com.intellij.openapi.util.TextRange {
        
        val document = com.intellij.psi.PsiDocumentManager.getInstance(usageFile.project).getDocument(usageFile)
        if (document != null) {
            val lineStartOffset = document.getLineStartOffset(usage.line - 1)
            val startOffset = lineStartOffset + usage.column - 1
            val endOffset = startOffset + usage.methodName.length
            return com.intellij.openapi.util.TextRange(startOffset, endOffset)
        }
        return com.intellij.openapi.util.TextRange.EMPTY_RANGE
    }
    
    override fun resolve(): PsiElement? = atomicValueElement
    
    override fun getCanonicalText(): String = usage.methodName
    
    override fun handleElementRename(newElementName: String): PsiElement {
        
        throw com.intellij.util.IncorrectOperationException("Renaming is not supported")
    }
    
    override fun bindToElement(element: PsiElement): PsiElement {
        throw com.intellij.util.IncorrectOperationException("Binding is not supported")
    }
    
    override fun isReferenceTo(element: PsiElement): Boolean {
        return element == atomicValueElement
    }
    
    override fun getVariants(): Array<Any> = emptyArray()
    
    override fun isSoft(): Boolean = false
}