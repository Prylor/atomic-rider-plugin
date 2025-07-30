package com.jetbrains.rider.plugins.atomic.services

// Removed DaemonCodeAnalyzer import - not needed
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.jetbrains.rider.plugins.atomic.psi.*
import com.jetbrains.rider.plugins.atomic.psi.impl.AtomicPsiImplUtil
import com.intellij.openapi.diagnostic.Logger
import kotlinx.coroutines.runBlocking

@Service(Service.Level.PROJECT)
class AtomicFileValidator(private val project: Project) {
    
    companion object {
        fun getInstance(project: Project): AtomicFileValidator {
            return project.getService(AtomicFileValidator::class.java)
        }
        
        private val LOG = Logger.getInstance(AtomicFileValidator::class.java)
    }
    
    fun hasErrors(atomicFile: AtomicFile): Boolean {
        return ReadAction.compute<Boolean, Exception> {
            val missingFields = checkRequiredFields(atomicFile)
            if (missingFields.isNotEmpty()) {
                LOG.info("AtomicFileValidator: Missing required fields: ${missingFields.joinToString(", ")}")
                return@compute true
            }
            
            if (hasTypeValidationErrors(atomicFile)) {
                LOG.info("AtomicFileValidator: Type validation errors found")
                return@compute true
            }
            
            if (hasDuplicates(atomicFile)) {
                LOG.info("AtomicFileValidator: Duplicate items found")
                return@compute true
            }
            
            
            false
        }
    }
    
    /**
     * Get validation errors for the atomic file
     */
    fun getValidationErrors(atomicFile: AtomicFile): List<String> {
        return ReadAction.compute<List<String>, Exception> {
            val errors = mutableListOf<String>()
            
            val missingFields = checkRequiredFields(atomicFile)
            if (missingFields.isNotEmpty()) {
                errors.add("Missing required fields: ${missingFields.joinToString(", ")}")
            }
            
            if (hasTypeValidationErrors(atomicFile)) {
                errors.add("Type validation errors found")
            }
            
            if (hasDuplicates(atomicFile)) {
                errors.add("Duplicate items found in file")
            }
            
            
            errors
        }
    }
    
    private fun checkRequiredFields(atomicFile: AtomicFile): List<String> {
        val missingFields = mutableListOf<String>()
        val existingFields = mutableSetOf<String>()
        
        atomicFile.children.filterIsInstance<com.jetbrains.rider.plugins.atomic.psi.AtomicHeaderSection>().forEach { headerSection ->
            headerSection.children.forEach { child ->
                when (child) {
                    is com.jetbrains.rider.plugins.atomic.psi.AtomicHeaderProp -> {
                        existingFields.add("header")
                        if (!hasValue(child)) missingFields.add("header")
                    }
                    is com.jetbrains.rider.plugins.atomic.psi.AtomicEntityTypeProp -> {
                        existingFields.add("entityType")
                        if (!hasValue(child)) missingFields.add("entityType")
                    }
                }
            }
        }
        
        fun checkElement(element: com.intellij.psi.PsiElement) {
            element.children.forEach { child ->
                when (child) {
                    is com.jetbrains.rider.plugins.atomic.psi.AtomicNamespaceProp -> {
                        existingFields.add("namespace")
                        if (!hasValue(child)) missingFields.add("namespace")
                    }
                    is com.jetbrains.rider.plugins.atomic.psi.AtomicClassNameProp -> {
                        existingFields.add("className")
                        if (!hasValue(child)) missingFields.add("className")
                    }
                    is com.jetbrains.rider.plugins.atomic.psi.AtomicDirectoryProp -> {
                        existingFields.add("directory")
                        if (!hasValue(child)) missingFields.add("directory")
                    }
                    is com.jetbrains.rider.plugins.atomic.psi.AtomicSolutionProp -> {
                        existingFields.add("solution")
                        if (!hasValue(child)) missingFields.add("solution")
                    }
                }
                checkElement(child)
            }
        }
        checkElement(atomicFile)
        
        val requiredFields = listOf("header", "entityType", "namespace", "className", "directory")
        requiredFields.forEach { field ->
            if (!existingFields.contains(field)) {
                missingFields.add(field)
            }
        }
        
        return missingFields
    }
    
    private fun hasValue(element: com.intellij.psi.PsiElement): Boolean {
        val text = element.text
        val colonIndex = text.indexOf(':')
        if (colonIndex == -1) return false
        
        val value = text.substring(colonIndex + 1).trim()
        return value.isNotEmpty()
    }
    
    private fun hasNamespaceErrors(atomicFile: AtomicFile): Boolean {
        return false
    }
    
    private fun hasTypeValidationErrors(atomicFile: AtomicFile): Boolean {
        val generationService = AtomicGenerationService.getInstance(project)
        if (!generationService.isReady()) {
            return false
        }
        
        val imports = atomicFile.children.filterIsInstance<AtomicImportsSection>().firstOrNull()?.let { importsSection ->
            importsSection.importItemList.mapNotNull { importItem ->
                importItem.node.findChildByType(AtomicTypes.IMPORT_PATH)?.text
            }
        } ?: emptyList()
        
        val valuesSection = atomicFile.children.filterIsInstance<AtomicValuesSection>().firstOrNull() ?: return false
        
        for (valueItem in valuesSection.valueItemList) {
            val typeReference = AtomicPsiImplUtil.getTypeReference(valueItem)
            if (typeReference != null && typeReference.isNotBlank()) {
                val primitiveTypes = setOf(
                    "bool", "byte", "sbyte", "char", "decimal", "double", "float",
                    "int", "uint", "long", "ulong", "short", "ushort", "string",
                    "object", "void", "dynamic"
                )
                
                val mainTypeName = extractMainTypeName(typeReference)
                if (primitiveTypes.contains(mainTypeName)) {
                    continue
                }
                
                val validationResult = runBlocking {
                    try {
                        generationService.validateType(mainTypeName, imports, project.basePath ?: "")
                    } catch (e: Exception) {
                        null
                    }
                }
                
                if (validationResult != null) {
                    if (!validationResult.isValid && !validationResult.isAmbiguous) {
                        return true 
                    }
                    if (validationResult.isAmbiguous) {
                        return true
                    }
                }
            }
        }
        
        return false
    }
    
    private fun extractMainTypeName(typeText: String): String {
        val genericStart = typeText.indexOf('<')
        val arrayStart = typeText.indexOf('[')
        
        val endIndex = when {
            genericStart == -1 && arrayStart == -1 -> typeText.length
            genericStart == -1 -> arrayStart
            arrayStart == -1 -> genericStart
            else -> minOf(genericStart, arrayStart)
        }
        
        return typeText.substring(0, endIndex).trim()
    }
    
    private fun hasDuplicates(atomicFile: AtomicFile): Boolean {
        val importsSection = atomicFile.children.filterIsInstance<AtomicImportsSection>().firstOrNull()
        if (importsSection != null) {
            val imports = importsSection.importItemList.mapNotNull { 
                it.node.findChildByType(AtomicTypes.IMPORT_PATH)?.text 
            }
            if (imports.size != imports.toSet().size) {
                return true
            }
        }
        
        val tagsSection = atomicFile.children.filterIsInstance<AtomicTagsSection>().firstOrNull()
        if (tagsSection != null) {
            val tags = tagsSection.tagItemList.mapNotNull { 
                it.node.findChildByType(AtomicTypes.TAG_NAME)?.text 
            }
            if (tags.size != tags.toSet().size) {
                return true
            }
        }
        
        val valuesSection = atomicFile.children.filterIsInstance<AtomicValuesSection>().firstOrNull()
        if (valuesSection != null) {
            val values = valuesSection.valueItemList.mapNotNull {
                it.node.findChildByType(AtomicTypes.VALUE_NAME)?.text
            }
            if (values.size != values.toSet().size) {
                return true
            }
        }
        
        return false
    }
}