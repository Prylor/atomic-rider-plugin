package com.jetbrains.rider.plugins.atomic.search

import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.openapi.application.ReadAction
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.util.Processor
import com.jetbrains.rider.plugins.atomic.psi.AtomicTagItem
import com.jetbrains.rider.plugins.atomic.psi.AtomicTypes
import com.jetbrains.rider.plugins.atomic.services.AtomicGenerationService
import kotlinx.coroutines.runBlocking
import com.intellij.psi.PsiManager
import com.intellij.openapi.vfs.LocalFileSystem

class AtomicTagUsageSearcher : QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters>(true) {
    
    override fun processQuery(parameters: ReferencesSearch.SearchParameters, consumer: Processor<in PsiReference>) {
        val element = parameters.elementToSearch
        
        println("[AtomicTagUsageSearcher] processQuery called:")
        println("  Element class: ${element.javaClass.name}")
        println("  Element text: '${element.text}'")
        println("  Node type: ${element.node?.elementType}")
        
        val tagName = when {
            element is AtomicTagItem -> {
                val tagNameNode = element.node.findChildByType(AtomicTypes.TAG_NAME)
                println("  -> Element is AtomicTagItem, name: ${tagNameNode?.text}")
                tagNameNode?.text
            }
            
            element.node?.elementType == AtomicTypes.TAG_NAME && element.parent is AtomicTagItem -> {
                println("  -> Element is TAG_NAME token")
                element.text
            }
            
            element.parent?.node?.elementType == AtomicTypes.TAG_NAME && element.parent?.parent is AtomicTagItem -> {
                println("  -> Element is leaf inside TAG_NAME")
                element.parent.text
            }
            
            else -> {
                println("  -> Element type not recognized")
                null
            }
        }
        
        if (tagName != null && tagName.isNotEmpty()) {
            println("  -> Found tag name: '$tagName'")
            val project = element.project
            
            val atomicFile = element.containingFile as? com.jetbrains.rider.plugins.atomic.psi.AtomicFile
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
                
                val methodNames = getGeneratedTagMethodNames(tagName)
                println("  -> Looking for methods: ${methodNames.joinToString()}")
                
                val usages = runBlocking {
                    service.findGeneratedTagUsages(tagName, methodNames, project.basePath ?: "", generatedFilePath)
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
                            val reference = SyntheticAtomicTagReference(element, psiFile, usage)
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
            println("  -> No tag name found")
        }
    }
    
    private fun getGeneratedTagMethodNames(tagName: String): List<String> {
        return listOf(
            "Has${tagName}Tag",
            "Add${tagName}Tag",
            "Del${tagName}Tag"
        )
    }
}

/**
 * A synthetic reference that represents a usage of a generated tag method
 */
class SyntheticAtomicTagReference(
    private val atomicTagElement: PsiElement,
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
    
    override fun resolve(): PsiElement? = atomicTagElement
    
    override fun getCanonicalText(): String = usage.methodName
    
    override fun handleElementRename(newElementName: String): PsiElement {
        throw com.intellij.util.IncorrectOperationException("Renaming is not supported")
    }
    
    override fun bindToElement(element: PsiElement): PsiElement {
        throw com.intellij.util.IncorrectOperationException("Binding is not supported")
    }
    
    override fun isReferenceTo(element: PsiElement): Boolean {
        return element == atomicTagElement
    }
    
    override fun getVariants(): Array<Any> = emptyArray()
    
    override fun isSoft(): Boolean = false
}